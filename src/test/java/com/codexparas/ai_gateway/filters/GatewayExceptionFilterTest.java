package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.execution.ProviderExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class GatewayExceptionFilterTest {
  @Test
  void convertsProviderFailureToSafeBadGatewayResponse() throws Exception {
    GatewayExceptionFilter filter = new GatewayExceptionFilter(new ObjectMapper());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest("POST", "/openai/chat/completions"),
        response,
        (request, ignoredResponse) -> {
          throw new ProviderExecutionException(
              "OPENAI", "gpt", new IllegalStateException("secret"));
        });

    assertThat(response.getStatus()).isEqualTo(502);
    assertThat(response.getContentAsString()).contains("provider_execution_failed");
    assertThat(response.getContentAsString()).doesNotContain("secret");
  }
}
