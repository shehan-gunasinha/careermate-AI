package com.careermate.applications;

import com.careermate.applications.ApplicationDtos.ApplicationDto;
import com.careermate.applications.ApplicationDtos.ApplicationRequest;
import com.careermate.applications.ApplicationDtos.HistoryDto;
import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.response.PageResponse;
import com.careermate.jobs.Job;
import com.careermate.jobs.JobRepository;
import com.careermate.notifications.NotificationService;
import com.careermate.notifications.NotificationType;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository repository;
    private final ApplicationHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ApplicationDto create(User user, ApplicationRequest req) {
        Application a = new Application();
        a.setUser(userRepository.getReferenceById(user.getId()));
        apply(a, req, true);
        ApplicationStatus status = req.status() == null ? ApplicationStatus.SAVED : req.status();
        a.setStatus(status);
        if (a.getApplicationDate() == null && status != ApplicationStatus.SAVED) {
            a.setApplicationDate(LocalDate.now());
        }
        a = repository.save(a);
        addHistory(a, null, status, "Application added");
        return toDto(a);
    }

    @Transactional
    public ApplicationDto update(User user, Long id, ApplicationRequest req) {
        Application a = owned(user, id);
        ApplicationStatus old = a.getStatus();
        apply(a, req, false);
        if (req.status() != null && req.status() != old) {
            a.setStatus(req.status());
        }
        a = repository.save(a);
        if (a.getStatus() != old) {
            addHistory(a, old, a.getStatus(), null);
            notifyStatus(user, a);
        }
        return toDto(a);
    }

    @Transactional
    public ApplicationDto changeStatus(User user, Long id, ApplicationStatus status, String note) {
        Application a = owned(user, id);
        ApplicationStatus old = a.getStatus();
        if (old != status) {
            a.setStatus(status);
            if (status == ApplicationStatus.APPLIED && a.getApplicationDate() == null) {
                a.setApplicationDate(LocalDate.now());
            }
            a = repository.save(a);
            addHistory(a, old, status, note);
            notifyStatus(user, a);
        }
        return toDto(a);
    }

    @Transactional
    public void delete(User user, Long id) {
        Application a = owned(user, id);
        historyRepository.deleteByApplicationId(a.getId());
        repository.delete(a);
    }

    @Transactional(readOnly = true)
    public ApplicationDto get(User user, Long id) {
        return toDto(owned(user, id));
    }

    @Transactional(readOnly = true)
    public List<HistoryDto> history(User user, Long id) {
        owned(user, id);
        return historyRepository.findByApplicationIdOrderByChangedAtDescIdDesc(id).stream()
                .map(h -> new HistoryDto(h.getId(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedAt())).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationDto> search(User user, ApplicationStatus status, String q, int page, int size, String sortBy, boolean asc) {
        Specification<Application> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("user").get("id"), user.getId()));
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("jobTitle")), like), cb.like(cb.lower(root.get("company")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "company" -> "company";
            case "jobTitle" -> "jobTitle";
            case "status" -> "status";
            case "applicationDate" -> "applicationDate";
            default -> "updatedAt";
        };
        Sort sort = Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, sortField).and(Sort.by(Sort.Direction.DESC, "id"));
        Page<Application> result = repository.findAll(spec, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)), sort));
        return PageResponse.from(result, this::toDto);
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Application a, ApplicationRequest req, boolean creating) {
        if (req.jobId() != null) {
            Job job = jobRepository.findById(req.jobId()).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
            a.setJob(job);
            if (isBlank(req.jobTitle()) && isBlank(a.getJobTitle())) a.setJobTitle(job.getTitle());
            if (isBlank(req.company()) && isBlank(a.getCompany())) a.setCompany(job.getCompanyName());
            if (isBlank(req.applicationUrl()) && isBlank(a.getApplicationUrl())) a.setApplicationUrl(job.getApplicationUrl());
        }
        if (!isBlank(req.jobTitle())) a.setJobTitle(req.jobTitle().trim());
        if (!isBlank(req.company())) a.setCompany(req.company().trim());
        if (isBlank(a.getJobTitle())) throw new BadRequestException("Job title is required");
        if (isBlank(a.getCompany())) throw new BadRequestException("Company is required");
        if (creating || req.applicationDate() != null) a.setApplicationDate(req.applicationDate());
        if (!isBlank(req.applicationUrl())) a.setApplicationUrl(req.applicationUrl().trim());
        a.setContactPerson(isBlank(req.contactPerson()) ? null : req.contactPerson().trim());
        a.setInterviewDate(req.interviewDate());
        a.setNotes(isBlank(req.notes()) ? null : req.notes().trim());
    }

    private void addHistory(Application a, ApplicationStatus from, ApplicationStatus to, String note) {
        ApplicationHistory h = new ApplicationHistory();
        h.setApplication(a);
        h.setFromStatus(from == null ? null : from.name());
        h.setToStatus(to.name());
        h.setNote(note);
        h.setChangedAt(Instant.now());
        historyRepository.save(h);
    }

    private void notifyStatus(User user, Application a) {
        notificationService.create(user, NotificationType.APPLICATION_STATUS, "Application status updated",
                a.getJobTitle() + " at " + a.getCompany() + " is now \"" + pretty(a.getStatus()) + "\".",
                "/applications", null);
    }

    public static String pretty(ApplicationStatus s) {
        String t = s.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private Application owned(User user, Long id) {
        return repository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    public ApplicationDto toDto(Application a) {
        return new ApplicationDto(a.getId(), a.getJob() == null ? null : a.getJob().getId(), a.getJobTitle(), a.getCompany(),
                a.getApplicationDate(), a.getStatus(), a.getApplicationUrl(), a.getContactPerson(), a.getInterviewDate(),
                a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
