package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.guardrail.GuardrailFactory;
import com.codexparas.ai_gateway.guardrail.impl.DlpGuardrail;
import com.codexparas.ai_gateway.guardrail.impl.PiiGuardrail;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GuardrailFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GuardrailFactory guardrailFactory =
      new GuardrailFactory(List.of(new PiiGuardrail(), new DlpGuardrail()));

  @Test
  void blocksConfiguredPiiGuardrail() throws Exception {
    GuardrailFilter filter = new GuardrailFilter(guardrailFactory, objectMapper);
    MockHttpServletRequest request = requestWithGuardrail("BLOCK");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getContentAsString()).contains("pii");
  }

  @Test
  void redactsConfiguredPiiGuardrailBeforeContinuing() throws Exception {
    GuardrailFilter filter = new GuardrailFilter(guardrailFactory, objectMapper);
    MockHttpServletRequest request = requestWithGuardrail("REDACT");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<HttpServletRequest> continuedRequest = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (nextRequest, ignoredResponse) -> continuedRequest.set((HttpServletRequest) nextRequest));

    assertThat(continuedRequest.get()).isNotNull();
    assertThat(
            continuedRequest.get().getAttribute(GatewayRequestAttributes.REQUEST_BODY).toString())
        .contains("[REDACTED]")
        .doesNotContain("person@example.com");
  }

  private MockHttpServletRequest requestWithGuardrail(String action) throws Exception {
    JsonNode guardrails =
        objectMapper.readTree(
            "[{\"id\":\"pii\",\"type\":\"PII_REDACTION\",\"stage\":\"REQUEST\",\"action\":\""
                + action
                + "\"}]\n");
    PreparedChatModel prepared =
        new PreparedChatModel(
            Mockito.mock(ChatModel.class),
            "gpt-5",
            "OPENAI",
            "gpt-5",
            null,
            null,
            null,
            null,
            null,
            null,
            guardrails);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL, prepared);
    request.setAttribute(
        GatewayRequestAttributes.REQUEST_BODY,
        "{\"model\":\"gpt-5\",\"email\":\"person@example.com\"}");
    return request;
  }
}
