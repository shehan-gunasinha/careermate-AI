package com.careermate.config;

import com.careermate.applications.ApplicationDtos.ApplicationRequest;
import com.careermate.applications.ApplicationService;
import com.careermate.applications.ApplicationStatus;
import com.careermate.jobs.Job;
import com.careermate.jobs.JobRepository;
import com.careermate.jobs.JobService;
import com.careermate.profile.EmploymentStatus;
import com.careermate.profile.ProfileDtos.EducationDto;
import com.careermate.profile.ProfileDtos.ExperienceDto;
import com.careermate.profile.ProfileDtos.PreferencesDto;
import com.careermate.profile.ProfileDtos.ProfileUpdateRequest;
import com.careermate.profile.ProfileService;
import com.careermate.user.Role;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a demo job seeker and an admin account on first start (disable with SEED_DEMO_DATA=false).
 * The demo jobs themselves come from the V2 Flyway migration. All demo data is fictional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final AppProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final JobRepository jobRepository;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Seed seed = props.getSeed();
        if (!seed.isEnabled()) {
            return;
        }
        if (!userRepository.existsByEmailIgnoreCase(seed.getAdminEmail())) {
            User admin = create("CareerMate Admin", seed.getAdminEmail(), seed.getAdminPassword(), Role.ADMIN);
            profileService.ensureProfile(admin);
            log.info("Seeded admin account {}", seed.getAdminEmail());
        }
        if (!userRepository.existsByEmailIgnoreCase(seed.getDemoEmail())) {
            User demo = create("Demo User", seed.getDemoEmail(), seed.getDemoPassword(), Role.JOB_SEEKER);
            profileService.updateProfile(demo, new ProfileUpdateRequest("Demo User", "+94 70 000 0000", "Colombo, Sri Lanka",
                    "Junior Software Engineer (demo profile)",
                    "Demo profile used to explore CareerMate AI. Seeking an entry-level software engineering role.",
                    EmploymentStatus.STUDENT, 1,
                    List.of("Java", "Spring Boot", "SQL", "REST APIs", "Git", "HTML", "CSS"),
                    List.of(new EducationDto(null, "Sample University (demo)", "BSc (Hons)", "Computer Science", 2020, 2024, null)),
                    List.of(new ExperienceDto(null, "Sample Software Ltd (demo)", "Software Engineering Intern",
                            LocalDate.now().minusYears(1), LocalDate.now().minusMonths(6), false,
                            "Demo entry: built and tested REST API endpoints for an internal tool."))));
            profileService.updatePreferences(demo, new PreferencesDto(List.of("Information Technology"),
                    List.of("Software Engineer", "Developer"), List.of("Colombo", "Remote")));
            List<Job> jobs = jobRepository.findAll(PageRequest.of(0, 4)).getContent();
            if (jobs.size() >= 3) {
                jobService.save(demo, jobs.get(0).getId());
                jobService.save(demo, jobs.get(1).getId());
                applicationService.create(demo, new ApplicationRequest(jobs.get(0).getId(), null, null, LocalDate.now().minusDays(5),
                        ApplicationStatus.APPLIED, null, null, null, "Sample application created by the demo seeder."));
                applicationService.create(demo, new ApplicationRequest(jobs.get(1).getId(), null, null, LocalDate.now().minusDays(12),
                        ApplicationStatus.UNDER_REVIEW, null, null, null, null));
                applicationService.create(demo, new ApplicationRequest(jobs.get(2).getId(), null, null, null,
                        ApplicationStatus.SAVED, null, null, null, null));
            }
            log.info("Seeded demo account {}", seed.getDemoEmail());
        }
    }

    private User create(String name, String email, String password, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        return userRepository.save(u);
    }
}
