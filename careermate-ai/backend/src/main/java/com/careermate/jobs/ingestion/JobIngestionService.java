package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.SyncResult;
import com.careermate.jobs.JobService;
import com.careermate.jobs.JobService.UpsertOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Pulls jobs from every enabled {@link ExternalJobSource}, normalises, de-duplicates and expires stale listings. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobIngestionService {
    private final List<ExternalJobSource> sources;
    private final JobService jobService;
    private final AppProperties props;
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    public List<SyncResult> syncAll() {
        if (!syncing.compareAndSet(false, true)) {
            log.info("Job sync already in progress - skipping this trigger");
            return List.of();
        }
        try {
            List<SyncResult> results = new ArrayList<>();
            for (ExternalJobSource source : sources) {
                if (!source.isEnabled()) {
                    continue;
                }
                int created = 0;
                int updated = 0;
                int dupes = 0;
                int fetched = 0;
                String error = null;
                try {
                    List<ExternalJobSource.ExternalJob> jobs = source.fetch(props.getJobs().getSync().getMaxPerSource());
                    fetched = jobs.size();
                    for (ExternalJobSource.ExternalJob ej : jobs) {
                        try {
                            UpsertOutcome o = jobService.upsertExternal(ej.job(), source.name(), ej.externalId());
                            switch (o) {
                                case CREATED -> created++;
                                case UPDATED -> updated++;
                                case DUPLICATE -> dupes++;
                            }
                        } catch (Exception e) {
                            log.warn("Skipping job {} from {}: {}", ej.externalId(), source.name(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    log.warn("Job sync failed for {}: {}", source.name(), error);
                }
                results.add(new SyncResult(source.name(), fetched, created, updated, dupes, error));
            }
            int expired = jobService.expireJobs(sources.stream().map(ExternalJobSource::name).toList(),
                    props.getJobs().getSync().getStaleAfterDays());
            log.info("Job sync finished: {} (expired {})", results, expired);
            return results;
        } finally {
            syncing.set(false);
        }
    }

    @Scheduled(cron = "${app.jobs.sync.cron}")
    public void scheduledSync() {
        if (props.getJobs().getSync().isEnabled()) {
            syncAll();
        } else {
            jobService.expireJobs(List.of(), props.getJobs().getSync().getStaleAfterDays());
        }
    }

    /**
     * Populate the database with real listings the first time the app starts so recommendations are not limited to
     * seeded demo jobs. Runs once in the background and is skipped when external jobs already exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        AppProperties.Sync sync = props.getJobs().getSync();
        if (!sync.isEnabled() || !sync.isSyncOnStartup() || jobService.hasExternalJobs()) {
            return;
        }
        log.info("No external jobs found - starting initial job sync");
        CompletableFuture.runAsync(() -> {
            try {
                syncAll();
            } catch (Exception e) {
                log.warn("Initial job sync failed: {}", e.getMessage());
            }
        });
    }
}
