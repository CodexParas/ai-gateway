package com.codexparas.ai_gateway.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codexparas.ai_gateway.config.GatewayExecutionProperties;
import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.model.ChatModelFactory;
import com.codexparas.ai_gateway.model.ModelConfigurationService;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class RequestExecutionFilterTest {
  @Test
  void executesNonStreamingPromptThroughPreparedChatModel() throws Exception {
    ChatModel chatModel = mock(ChatModel.class);
    ChatResponse expected =
        new ChatResponse(List.of(new Generation(new AssistantMessage("answer"))));
    when(chatModel.call(any(Prompt.class))).thenReturn(expected);
    RequestTransformer transformer = mock(RequestTransformer.class);
    when(transformer.provider()).thenReturn("OPENAI");
    when(transformer.supports(any())).thenReturn(true);
    when(transformer.transform(any(), any()))
        .thenReturn(new TransformedChatRequest(List.of(new UserMessage("hello")), null));
    RequestExecutionFilter filter =
        new RequestExecutionFilter(
            new ObjectMapper(),
            List.of(transformer),
            new GatewayExecutionProperties(1),
            mock(ModelConfigurationService.class),
            mock(ChatModelFactory.class));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAttribute(
        GatewayRequestAttributes.ROUTE, new GatewayRoute("OPENAI", "chat.completions", false));
    request.setAttribute(
        GatewayRequestAttributes.PREPARED_CHAT_MODEL,
        new PreparedChatModel(
            chatModel, "gpt", "OPENAI", "gpt", null, null, null, null, null, null, null));
    request.setAttribute(
        GatewayRequestAttributes.REQUEST_BODY, "{\"model\":\"gpt\",\"messages\":[{}]}");
    AtomicBoolean continued = new AtomicBoolean();

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (ignoredRequest, ignoredResponse) -> continued.set(true));

    assertThat(continued).isTrue();
    assertThat(request.getAttribute(GatewayRequestAttributes.CHAT_RESPONSE)).isSameAs(expected);
  }
}
