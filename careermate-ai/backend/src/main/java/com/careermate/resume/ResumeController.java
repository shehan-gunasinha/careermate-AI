package com.careermate.resume;

import com.careermate.common.response.ApiResponse;
import com.careermate.resume.ResumeDtos.AnalysisDto;
import com.careermate.resume.ResumeDtos.DownloadFile;
import com.careermate.resume.ResumeDtos.ResumeBuildRequest;
import com.careermate.resume.ResumeDtos.ResumeDto;
import com.careermate.resume.ResumeDtos.SuggestRequest;
import com.careermate.resume.ResumeDtos.SuggestResponse;
import com.careermate.user.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeDto> upload(@AuthenticationPrincipal User user,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestParam(value = "title", required = false) String title) {
        return ApiResponse.ok("Resume uploaded", resumeService.upload(user, file, title));
    }

    @GetMapping
    public ApiResponse<List<ResumeDto>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(resumeService.list(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeDto> get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(resumeService.get(user, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        resumeService.delete(user, id);
        return ApiResponse.ok("Resume deleted", null);
    }

    @PostMapping("/{id}/analyze")
    public ApiResponse<AnalysisDto> analyze(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok("Analysis complete", resumeService.analyze(user, id));
    }

    @GetMapping("/{id}/analysis")
    public ApiResponse<AnalysisDto> analysis(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(resumeService.latestAnalysis(user, id));
    }

    @GetMapping("/{id}/analyses")
    public ApiResponse<List<AnalysisDto>> history(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(resumeService.history(user, id));
    }

    @PostMapping("/build")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeDto> build(@AuthenticationPrincipal User user, @Valid @RequestBody ResumeBuildRequest req) {
        return ApiResponse.ok("Resume created", resumeService.build(user, req));
    }

    @PostMapping("/suggest")
    public ApiResponse<SuggestResponse> suggest(@AuthenticationPrincipal User user, @Valid @RequestBody SuggestRequest req) {
        return ApiResponse.ok(resumeService.suggest(user, req));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal User user, @PathVariable Long id) {
        DownloadFile f = resumeService.download(user, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.filename()).build().toString())
                .contentType(MediaType.parseMediaType(f.contentType()))
                .body(f.bytes());
    }
}
