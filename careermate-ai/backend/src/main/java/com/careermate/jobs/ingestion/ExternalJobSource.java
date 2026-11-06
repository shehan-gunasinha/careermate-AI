package com.careermate.jobs.ingestion;

import com.careermate.jobs.JobDtos.JobCreateRequest;
import java.util.List;

/** A legitimate external job feed/API. Add new implementations as Spring beans to extend ingestion. */
public interface ExternalJobSource {
    /** Stable upper-case identifier stored in jobs.source. */
    String name();

    boolean isEnabled();

    List<ExternalJob> fetch(int maxItems) throws Exception;

    record ExternalJob(String externalId, JobCreateRequest job) {
    }
}
