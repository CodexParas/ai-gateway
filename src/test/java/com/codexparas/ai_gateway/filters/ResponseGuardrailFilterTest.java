package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.guardrail.GuardrailFactory;
import com.codexparas.ai_gateway.guardrail.impl.ContentSafetyGuardrail;
import com.codexparas.ai_gateway.guardrail.impl.DlpGuardrail;
import com.codexparas.ai_gateway.guardrail.impl.NoOpGuardrail;
import com.codexparas.ai_gateway.guardrail.impl.PiiGuardrail;
import com.codexparas.ai_gateway.guardrail.impl.PromptInjectionGuardrail;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class ResponseGuardrailFilterTest {
  @Test
  void blocksResponseContainingConfiguredPii() throws Exception {
    ResponseGuardrailFilter filter =
        new ResponseGuardrailFilter(
            new GuardrailFactory(
                List.of(
                    new ContentSafetyGuardrail(),
                    new DlpGuardrail(),
                    new NoOpGuardrail(),
                    new PiiGuardrail(),
                    new PromptInjectionGuardrail())),
            new ObjectMapper());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAttribute(
        GatewayRequestAttributes.PREPARED_CHAT_MODEL,
        new PreparedChatModel(
            null,
            "gpt",
            "OPENAI",
            "gpt",
            null,
            null,
            null,
            null,
            null,
            null,
            new ObjectMapper()
                .readTree(
                    "[{\"id\":\"pii\",\"type\":\"PII_REDACTION\",\"stage\":\"RESPONSE\",\"action\":\"BLOCK\"}]")));
    request.setAttribute(
        GatewayRequestAttributes.CHAT_RESPONSE,
        new ChatResponse(List.of(new Generation(new AssistantMessage("email test@example.com")))));
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean continued = new AtomicBoolean();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

    assertThat(continued).isFalse();
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getContentAsString()).contains("response_guardrail_blocked");
  }
}
