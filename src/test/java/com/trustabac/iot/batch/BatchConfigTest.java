package com.trustabac.iot.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BatchConfigTest {

    @Autowired
    private Job offlineAuditJob;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Test
    @DisplayName("Verify Spring Batch core beans and offlineAuditJob are loaded")
    void testBatchBeansLoaded() {
        assertNotNull(offlineAuditJob, "offlineAuditJob must be configured");
        assertNotNull(jobRepository, "jobRepository must be configured");
        assertNotNull(jobLauncher, "jobLauncher must be configured");
    }
}
