package com.careermate.jobs;

import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ConflictException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.response.PageResponse;
import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.ImportResult;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.careermate.jobs.JobDtos.JobDto;
import com.careermate.jobs.JobDtos.JobSearchCriteria;
import com.careermate.matching.CandidateContext;
import com.careermate.matching.CandidateContextService;
import com.careermate.matching.MatchResult;
import com.careermate.matching.MatchingService;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    public enum UpsertOutcome { CREATED, UPDATED, DUPLICATE }

    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final CandidateContextService contextService;
    private final AppProperties props;

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<JobDto> search(User user, JobSearchCriteria criteria, Pageable pageable) {
        Page<Job> page = jobRepository.findAll(JobSpecifications.from(criteria), pageable);
        Set<Long> saved = savedJobRepository.findJobIdsByUserId(user.getId());
        return PageResponse.from(page, j -> toDto(j, saved.contains(j.getId()), null));
    }

    @Transactional(readOnly = true)
    public JobDto get(User user, Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        boolean saved = savedJobRepository.findByUserIdAndJobId(user.getId(), id).isPresent();
        return toDto(job, saved, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobDto> recommended(User user, int page, int size) {
        CandidateContext ctx = contextService.build(user);
        MatchingService.PreparedCandidate prepared = matchingService.prepare(ctx);
        Set<Long> saved = savedJobRepository.findJobIdsByUserId(user.getId());
        List<Job> open = jobRepository.findOpenJobs(LocalDate.now(), PageRequest.of(0, 300));
        Map<Long, Integer> scores = new HashMap<>();
        for (Job j : open) {
            scores.put(j.getId(), matchingService.quickScore(ctx, prepared, j));
        }
        Comparator<Job> byScore = Comparator.<Job>comparingInt(j -> scores.get(j.getId())).reversed();
        // Real listings stay ahead of demo fallback jobs, but demo jobs are kept so the feed is never empty.
        Comparator<Job> order = props.getJobs().getSync().isPreferExternal()
                ? Comparator.<Job, Boolean>comparing(Job::isDemo).thenComparing(byScore)
                : byScore;
        List<JobDto> ranked = open.stream()
                .sorted(order)
                .map(j -> toDto(j, saved.contains(j.getId()), scores.get(j.getId())))
                .toList();
        return PageResponse.of(ranked, Math.max(0, page), Math.max(1, Math.min(size, 50)));
    }

    public MatchResult match(User user, Long jobId) {
        return matchingService.match(user, jobId);
    }

    @Transactional
    public void save(User user, Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (savedJobRepository.findByUserIdAndJobId(user.getId(), jobId).isPresent()) {
            return;
        }
        SavedJob s = new SavedJob();
        s.setUser(userRepository.getReferenceById(user.getId()));
        s.setJob(job);
        savedJobRepository.save(s);
    }

    @Transactional
    public void unsave(User user, Long jobId) {
        savedJobRepository.deleteByUserIdAndJobId(user.getId(), jobId);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobDto> saved(User user, Pageable pageable) {
        Page<SavedJob> page = savedJobRepository.findByUserId(user.getId(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(page, s -> toDto(s.getJob(), true, null));
    }

    @Transactional(readOnly = true)
    public List<JobDto> latest(User user, int limit) {
        Set<Long> saved = savedJobRepository.findJobIdsByUserId(user.getId());
        return jobRepository.findOpenJobs(LocalDate.now(), PageRequest.of(0, limit)).stream()
                .map(j -> toDto(j, saved.contains(j.getId()), null)).toList();
    }

    // ------------------------------------------------------------------ admin / ingestion

    @Transactional(readOnly = true)
    public boolean hasExternalJobs() {
        return jobRepository.countByDemoFalseAndActiveTrue() > 0;
    }

    @Transactional
    public JobDto create(JobCreateRequest req, String defaultSource) {
        String source = req.source() == null || req.source().isBlank() ? defaultSource : req.source().trim().toUpperCase(Locale.ROOT);
        Job job = build(req, source, null);
        if (jobRepository.existsByFingerprint(job.getFingerprint())) {
            throw new ConflictException("A similar job (same title, company and location) already exists");
        }
        return toDto(jobRepository.save(job), false, null);
    }

    @Transactional
    public ImportResult importJobs(List<JobCreateRequest> requests) {
        int created = 0;
        int dupes = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            JobCreateRequest r = requests.get(i);
            try {
                String source = r.source() == null || r.source().isBlank() ? "IMPORT" : r.source().trim().toUpperCase(Locale.ROOT);
                Job job = build(r, source, null);
                if (jobRepository.existsByFingerprint(job.getFingerprint())) {
                    dupes++;
                } else {
                    jobRepository.save(job);
                    created++;
                }
            } catch (BadRequestException e) {
                errors.add("Item " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, dupes, errors);
    }

    @Transactional
    public UpsertOutcome upsertExternal(JobCreateRequest req, String source, String externalId) {
        Job existing = jobRepository.findBySourceAndExternalId(source, externalId).orElse(null);
        if (existing != null) {
            existing.setLastSyncedAt(Instant.now());
            existing.setActive(true);
            existing.setDeadline(req.deadline());
            jobRepository.save(existing);
            return UpsertOutcome.UPDATED;
        }
        Job job = build(req, source, externalId);
        if (jobRepository.existsByFingerprint(job.getFingerprint())) {
            return UpsertOutcome.DUPLICATE;
        }
        jobRepository.save(job);
        return UpsertOutcome.CREATED;
    }

    @Transactional
    public void deactivate(Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setActive(false);
        jobRepository.save(job);
    }

    @Transactional
    public int expireJobs(List<String> externalSources, int staleAfterDays) {
        int n = jobRepository.deactivatePastDeadline(LocalDate.now());
        if (!externalSources.isEmpty()) {
            n += jobRepository.deactivateStaleExternal(externalSources, Instant.now().minusSeconds(staleAfterDays * 86400L));
        }
        return n;
    }

    // ------------------------------------------------------------------ mapping

    private Job build(JobCreateRequest r, String source, String externalId) {
        if (r.salaryMin() != null && r.salaryMax() != null && r.salaryMin().compareTo(r.salaryMax()) > 0) {
            throw new BadRequestException("Minimum salary cannot exceed maximum salary");
        }
        Job j = new Job();
        j.setTitle(r.title().trim());
        j.setCompanyName(r.companyName().trim());
        j.setCompanyLogoUrl(blank(r.companyLogoUrl()));
        j.setCompanyDescription(blank(r.companyDescription()));
        j.setLocation(r.location().trim());
        j.setIndustry(blank(r.industry()));
        j.setEmploymentType(r.employmentType());
        j.setWorkArrangement(r.workArrangement());
        j.setExperienceLevel(r.experienceLevel());
        j.setSalaryMin(r.salaryMin());
        j.setSalaryMax(r.salaryMax());
        j.setSalaryCurrency(blank(r.salaryCurrency()));
        j.setDescription(r.description().trim());
        String shortDesc = blank(r.shortDescription());
        if (shortDesc == null) {
            String d = j.getDescription();
            shortDesc = d.length() > 240 ? d.substring(0, 237) + "..." : d;
        }
        j.setShortDescription(shortDesc);
        j.setResponsibilities(blank(r.responsibilities()));
        j.setRequiredQualifications(blank(r.requiredQualifications()));
        j.setPreferredQualifications(blank(r.preferredQualifications()));
        j.setPostedDate(r.postedDate() == null ? LocalDate.now() : r.postedDate());
        j.setDeadline(r.deadline());
        j.setApplicationUrl(blank(r.applicationUrl()));
        j.setSource(source);
        j.setExternalId(externalId);
        j.setDemo("DEMO".equals(source));
        j.setActive(true);
        j.setLastSyncedAt(Instant.now());
        j.setFingerprint(fingerprint(j));
        Map<String, Boolean> skills = new LinkedHashMap<>();
        if (r.requiredSkills() != null) r.requiredSkills().stream().filter(s -> s != null && !s.isBlank()).forEach(s -> skills.putIfAbsent(s.trim(), true));
        if (r.preferredSkills() != null) r.preferredSkills().stream().filter(s -> s != null && !s.isBlank()).forEach(s -> skills.putIfAbsent(s.trim(), false));
        skills.forEach((name, required) -> j.getSkills().add(new JobSkill(j, name, required)));
        return j;
    }

    public JobDto toDto(Job j, boolean saved, Integer score) {
        List<String> req = j.getSkills().stream().filter(JobSkill::isRequired).map(JobSkill::getSkill).toList();
        List<String> pref = j.getSkills().stream().filter(s -> !s.isRequired()).map(JobSkill::getSkill).toList();
        return new JobDto(j.getId(), j.getTitle(), j.getCompanyName(), j.getCompanyLogoUrl(), j.getCompanyDescription(),
                j.getLocation(), j.getIndustry(), j.getEmploymentType(), j.getWorkArrangement(), j.getExperienceLevel(),
                j.getSalaryMin(), j.getSalaryMax(), j.getSalaryCurrency(), j.getShortDescription(), j.getDescription(),
                j.getResponsibilities(), j.getRequiredQualifications(), j.getPreferredQualifications(), req, pref,
                j.getPostedDate(), j.getDeadline(), j.getApplicationUrl(), j.getSource(), j.isDemo(), j.isActive(), saved, score);
    }

    public static String fingerprint(Job j) {
        String raw = (j.getTitle() + "|" + j.getCompanyName() + "|" + j.getLocation()).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
