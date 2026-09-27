package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.security.UpstreamCredential;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class UpstreamCredentialScopeFilterTest {

  @Test
  void rejectsModelOutsideCredentialAllowList() throws Exception {
    UpstreamCredentialScopeFilter filter = new UpstreamCredentialScopeFilter(new ObjectMapper());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAttribute(
        GatewayRequestAttributes.UPSTREAM_CREDENTIAL,
        new UpstreamCredential(
            "credential", "org", "app", true, null, List.of("inference"), List.of("gpt-4")));
    request.setAttribute(GatewayRequestAttributes.MODEL_NAME, "gpt-5");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("model_not_allowed");
  }
}
