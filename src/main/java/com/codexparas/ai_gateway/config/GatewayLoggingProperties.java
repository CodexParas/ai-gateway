package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.logging")
public record GatewayLoggingProperties(
    boolean enabled,
    String topic,
    String systemName,
    boolean includeRequestBody,
    boolean includeResponseBody,
    int purgeDays) {}
