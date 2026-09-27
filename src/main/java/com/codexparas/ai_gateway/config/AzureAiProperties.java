package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.azure-ai")
public record AzureAiProperties(String endpoint, String apiKey, String apiVersion) {}
