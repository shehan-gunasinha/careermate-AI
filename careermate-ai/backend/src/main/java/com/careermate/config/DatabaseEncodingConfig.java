package com.careermate.config;

import com.careermate.common.util.DatabaseText;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Detects the database encoding so {@link DatabaseText} can keep non-UTF-8 databases writable. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseEncodingConfig {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void detectEncoding() {
        try {
            String encoding = jdbcTemplate.queryForObject(
                    "select pg_encoding_to_char(encoding) from pg_database where datname = current_database()",
                    String.class);
            DatabaseText.configure(encoding);
            log.info("Database text encoding: {}", encoding);
        } catch (Exception e) {
            log.debug("Could not detect the database text encoding; assuming UTF-8", e);
        }
    }
}
