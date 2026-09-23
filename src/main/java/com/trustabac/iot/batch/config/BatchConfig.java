package com.trustabac.iot.batch.config;

import com.trustabac.iot.batch.listener.BatchAuditJobExecutionListener;
import com.trustabac.iot.batch.step.AuthorizationAnalyticsTasklet;
import com.trustabac.iot.batch.step.DeviceAnalyticsTasklet;
import com.trustabac.iot.batch.step.SecurityAnalyticsTasklet;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration building the modular offline audit and analytics pipeline.
 */
@Configuration
public class BatchConfig {

    @Bean
    public Step authorizationAnalyticsStep(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager,
                                           AuthorizationAnalyticsTasklet tasklet) {
        return new StepBuilder("authorizationAnalyticsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step deviceAnalyticsStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    DeviceAnalyticsTasklet tasklet) {
        return new StepBuilder("deviceAnalyticsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step securityAnalyticsStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      SecurityAnalyticsTasklet tasklet) {
        return new StepBuilder("securityAnalyticsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job offlineAuditJob(JobRepository jobRepository,
                               BatchAuditJobExecutionListener listener,
                               Step authorizationAnalyticsStep,
                               Step deviceAnalyticsStep,
                               Step securityAnalyticsStep) {
        return new JobBuilder("offlineAuditJob", jobRepository)
                .listener(listener)
                .start(authorizationAnalyticsStep)
                .next(deviceAnalyticsStep)
                .next(securityAnalyticsStep)
                .build();
    }
}
