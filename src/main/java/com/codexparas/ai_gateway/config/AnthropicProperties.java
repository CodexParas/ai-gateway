package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.anthropic")
public record AnthropicProperties(String baseUrl, String apiKey, String version) {}
