package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.model-config")
public record ModelConfigurationProperties(String redisHash) {

  public ModelConfigurationProperties {
    if (redisHash == null || redisHash.isBlank()) {
      redisHash = "ai-gateway:model-config";
    }
  }
}
