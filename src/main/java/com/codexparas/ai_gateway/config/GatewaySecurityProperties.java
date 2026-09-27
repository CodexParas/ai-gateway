package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.security")
public record GatewaySecurityProperties(boolean enabled, String credentialHash) {
  public GatewaySecurityProperties {
    if (credentialHash == null || credentialHash.isBlank()) {
      credentialHash = "ai-gateway:credentials";
    }
  }
}
