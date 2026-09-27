package com.codexparas.ai_gateway.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bounded gateway execution policy. Streaming requests are always single-attempt.
 */
@Validated
@ConfigurationProperties("ai.gateway.execution")
public record GatewayExecutionProperties(@Min(1) @Max(5) int maxAttempts) {
    public GatewayExecutionProperties {
        if (maxAttempts < 1) maxAttempts = 1;
    }
}
