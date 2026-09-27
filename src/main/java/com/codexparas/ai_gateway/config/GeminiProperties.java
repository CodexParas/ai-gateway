package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.gemini")
public record GeminiProperties(String baseUrl, String apiKey, String apiVersion) {}
