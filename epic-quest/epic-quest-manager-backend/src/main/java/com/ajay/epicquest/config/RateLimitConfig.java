package com.ajay.epicquest.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    @Value("${rate.limit.auth.capacity:5}")
    private int capacity;

    @Value("${rate.limit.auth.refill-tokens:5}")
    private int refillTokens;

    @Value("${rate.limit.auth.refill-period:1}")
    private int refillPeriod;

    @Value("${rate.limit.auth.refill-unit:MINUTES}")
    private String refillUnit;

    @Bean
    public ConcurrentHashMap<String, Bucket> rateLimitCache() {
        return new ConcurrentHashMap<>();
    }

    public Bandwidth getBandwidth() {
        Duration duration = switch (refillUnit.toUpperCase()) {
            case "SECONDS" -> Duration.ofSeconds(refillPeriod);
            case "MINUTES" -> Duration.ofMinutes(refillPeriod);
            case "HOURS" -> Duration.ofHours(refillPeriod);
            default -> Duration.ofMinutes(refillPeriod);
        };

        return Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, duration)
                .build();
    }
}