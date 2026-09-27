package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClientConfiguration {

  @Bean("openAiWebClient")
  WebClient openAiWebClient(WebClient.Builder builder, OpenAiProperties properties) {
    return builder
        .baseUrl(properties.baseUrl())
        .defaultHeaders(headers -> addDefaultHeaders(headers, properties))
        .build();
  }

  private static void addDefaultHeaders(HttpHeaders headers, OpenAiProperties properties) {
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
      headers.setBearerAuth(properties.apiKey());
    }
  }
}
