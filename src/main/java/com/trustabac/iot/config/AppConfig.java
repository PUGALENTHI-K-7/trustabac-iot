package com.trustabac.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * General application configuration providing core beans such as the system clock.
 */
@Configuration
public class AppConfig {

    /**
     * Authoritative system clock for timestamp generation across services.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
