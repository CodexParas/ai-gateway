package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AnthropicProperties.class)
public class AnthropicClientConfiguration {

  @Bean("anthropicWebClient")
  WebClient anthropicWebClient(WebClient.Builder builder, AnthropicProperties properties) {
    return builder
        .baseUrl(properties.baseUrl())
        .defaultHeaders(
            headers -> {
              headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
              headers.set("anthropic-version", properties.version());
              if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                headers.set("x-api-key", properties.apiKey());
              }
            })
        .build();
  }
}
