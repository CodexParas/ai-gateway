package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AzureAiProperties.class)
public class AzureAiClientConfiguration {

  @Bean("azureAiWebClient")
  WebClient azureAiWebClient(WebClient.Builder builder, AzureAiProperties properties) {
    return builder
        .baseUrl(properties.endpoint())
        .defaultHeader("api-key", properties.apiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();
  }
}
