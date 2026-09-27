package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClientConfiguration {

  @Bean("geminiWebClient")
  WebClient geminiWebClient(WebClient.Builder builder, GeminiProperties properties) {
    return builder
        .baseUrl(properties.baseUrl() + "/" + properties.apiVersion())
        .defaultHeader("x-goog-api-key", properties.apiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();
  }
}
