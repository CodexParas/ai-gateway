package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.openai")
public record OpenAiProperties(String baseUrl, String apiKey) {}
