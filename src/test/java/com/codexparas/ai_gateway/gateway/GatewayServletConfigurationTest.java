package com.codexparas.ai_gateway.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GatewayServletConfigurationTest {
  @Test
  void registersAllCanonicalProviderMappings() {
    var registration = new GatewayServletConfiguration().gatewayFallbackServlet(new ObjectMapper());

    assertThat(registration.getUrlMappings())
        .containsExactlyInAnyOrder(
            "/openai/*", "/anthropic/*", "/bedrock/*", "/google/*", "/azure/*");
  }
}
