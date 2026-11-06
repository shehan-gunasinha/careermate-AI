package com.careermate.resume;

import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.util.Json;
import com.careermate.notifications.NotificationService;
import com.careermate.notifications.NotificationType;
import com.careermate.resume.ResumeAiService.AnalysisResult;
import com.careermate.resume.ResumeDtos.AnalysisDto;
import com.careermate.resume.ResumeDtos.DownloadFile;
import com.careermate.resume.ResumeDtos.ResumeBuildRequest;
import com.careermate.resume.ResumeDtos.ResumeDto;
import com.careermate.resume.ResumeDtos.SuggestRequest;
import com.careermate.resume.ResumeDtos.SuggestResponse;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("pdf", "docx");
    private static final int MAX_RESUMES_PER_USER = 25;

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final TextExtractor extractor;
    private final ResumeAiService aiService;
    private final ResumePdfBuilder pdfBuilder;
    private final NotificationService notificationService;

    @Transactional
    public ResumeDto upload(User user, MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File is too large. Maximum allowed size is 5 MB.");
        }
        if (resumeRepository.countByUserId(user.getId()) >= MAX_RESUMES_PER_USER) {
            throw new BadRequestException("You have reached the maximum of " + MAX_RESUMES_PER_USER + " stored resumes. Delete one first.");
        }
        String original = sanitizeName(file.getOriginalFilename());
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw new BadRequestException("Only PDF and DOCX files are supported");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read");
        }
        verifyMagicBytes(bytes, ext);
        String text = extractor.extract(bytes, ext);
        if (text.isBlank()) {
            throw new BadRequestException("No text could be extracted. Scanned or image-only files are not supported; please upload a text-based PDF or DOCX.");
        }
        Resume r = new Resume();
        r.setUser(userRepository.getReferenceById(user.getId()));
        r.setTitle(title == null || title.isBlank() ? stripExt(original) : title.trim().substring(0, Math.min(title.trim().length(), 200)));
        r.setOriginalFilename(original);
        r.setStoragePath(storage.store(user.getId(), bytes, ext));
        r.setContentType("pdf".equals(ext) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        r.setSizeBytes(bytes.length);
        r.setSource(ResumeSource.UPLOAD);
        r.setExtractedText(text);
        return toDto(resumeRepository.save(r), false);
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> list(User user) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(r -> toDto(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public ResumeDto get(User user, Long id) {
        return toDto(owned(user, id), true);
    }

    @Transactional
    public void delete(User user, Long id) {
        Resume r = owned(user, id);
        analysisRepository.deleteByResumeId(r.getId());
        resumeRepository.delete(r);
        storage.delete(r.getStoragePath());
    }

    @Transactional
    public AnalysisDto analyze(User user, Long id) {
        Resume r = owned(user, id);
        if (r.getExtractedText() == null || r.getExtractedText().isBlank()) {
            throw new BadRequestException("This resume has no extracted text to analyse");
        }
        String ext = r.getContentType().contains("pdf") ? "pdf" : "docx";
        AnalysisResult result = aiService.analyze(r.getExtractedText(), ext);
        AnalysisData d = result.data();
        ResumeAnalysis a = new ResumeAnalysis();
        a.setResume(r);
        a.setOverallScore(d.overall());
        a.setSectionScores(Json.write(d.sections()));
        a.setStrengths(Json.write(d.strengths()));
        a.setWeaknesses(Json.write(d.weaknesses()));
        a.setMissingInformation(Json.write(d.missing()));
        a.setRecommendations(Json.write(d.recommendations()));
        a.setLanguageIssues(Json.write(d.languageIssues()));
        a.setSummary(d.summary());
        a.setAiPowered(result.aiPowered());
        a = analysisRepository.save(a);
        notificationService.create(user, NotificationType.RESUME_REMINDER, "Resume analysis ready",
                "\"" + r.getTitle() + "\" scored " + d.overall() + "/100. Review the recommendations to improve it.",
                "/resumes/" + r.getId() + "/analysis", "resume-analysis-" + a.getId());
        return toAnalysisDto(a);
    }

    @Transactional(readOnly = true)
    public AnalysisDto latestAnalysis(User user, Long id) {
        owned(user, id);
        return analysisRepository.findFirstByResumeIdOrderByCreatedAtDescIdDesc(id).map(this::toAnalysisDto)
                .orElseThrow(() -> new ResourceNotFoundException("This resume has not been analysed yet"));
    }

    @Transactional(readOnly = true)
    public List<AnalysisDto> history(User user, Long id) {
        owned(user, id);
        return analysisRepository.findByResumeIdOrderByCreatedAtDescIdDesc(id).stream().map(this::toAnalysisDto).toList();
    }

    @Transactional
    public ResumeDto build(User user, ResumeBuildRequest req) {
        byte[] pdf = pdfBuilder.build(req);
        Resume r = new Resume();
        r.setUser(userRepository.getReferenceById(user.getId()));
        r.setTitle(req.title().trim());
        r.setOriginalFilename(safeFileBase(req.title()) + ".pdf");
        r.setStoragePath(storage.store(user.getId(), pdf, "pdf"));
        r.setContentType("application/pdf");
        r.setSizeBytes(pdf.length);
        r.setSource(ResumeSource.BUILT);
        r.setExtractedText(pdfBuilder.toPlainText(req));
        return toDto(resumeRepository.save(r), false);
    }

    @Transactional(readOnly = true)
    public DownloadFile download(User user, Long id) {
        Resume r = owned(user, id);
        return new DownloadFile(storage.load(r.getStoragePath()), r.getOriginalFilename(), r.getContentType());
    }

    public SuggestResponse suggest(SuggestRequest req) {
        return aiService.suggest(req);
    }

    // ------------------------------------------------------------------ helpers

    private Resume owned(User user, Long id) {
        return resumeRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }

    private ResumeDto toDto(Resume r, boolean detail) {
        Integer score = analysisRepository.findFirstByResumeIdOrderByCreatedAtDescIdDesc(r.getId()).map(ResumeAnalysis::getOverallScore).orElse(null);
        return new ResumeDto(r.getId(), r.getTitle(), r.getOriginalFilename(), r.getContentType(), r.getSizeBytes(), r.getSource(),
                r.getCreatedAt(), score, detail ? extractor.info(r.getExtractedText()) : null, detail ? r.getExtractedText() : null);
    }

    private AnalysisDto toAnalysisDto(ResumeAnalysis a) {
        return new AnalysisDto(a.getId(), a.getResume().getId(), a.getOverallScore(), Json.readIntMap(a.getSectionScores()),
                Json.readStringList(a.getStrengths()), Json.readStringList(a.getWeaknesses()),
                Json.readStringList(a.getMissingInformation()), Json.readStringList(a.getRecommendations()),
                Json.readStringList(a.getLanguageIssues()), a.getSummary(), a.isAiPowered(), a.getCreatedAt());
    }

    private static void verifyMagicBytes(byte[] b, String ext) {
        boolean ok;
        if ("pdf".equals(ext)) {
            ok = b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
        } else {
            ok = b.length > 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
        }
        if (!ok) {
            throw new BadRequestException("The file content does not match its extension");
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "resume";
        }
        String n = name.replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i <= 0 ? name : name.substring(0, i);
    }

    private static String safeFileBase(String title) {
        String s = title.trim().replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", "_");
        return s.isEmpty() ? "resume" : s.substring(0, Math.min(s.length(), 80));
    }
}
