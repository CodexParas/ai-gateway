package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.bedrock")
public record BedrockProperties(String region) {}
