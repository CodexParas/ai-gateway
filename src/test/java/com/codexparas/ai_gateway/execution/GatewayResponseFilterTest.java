package com.codexparas.ai_gateway.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.config.GatewayStreamingProperties;
import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.route.GatewayRoute;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

class GatewayResponseFilterTest {
  @Test
  void writesOpenAiStreamChunk() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    GatewayResponseFilter filter =
        new GatewayResponseFilter(
            objectMapper,
            new GatewayStreamingProperties(Duration.ofSeconds(5)),
            List.of(new OpenAiResponseTransformer(objectMapper)),
            List.of(new OpenAiResponseTransformer(objectMapper)));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAsyncSupported(true);
    request.setAttribute(
        GatewayRequestAttributes.ROUTE, new GatewayRoute("OPENAI", "chat.completions", true));
    request.setAttribute(
        GatewayRequestAttributes.CHAT_RESPONSE_STREAM,
        Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("hello"))))));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getContentAsString()).contains("chat.completion.chunk");
  }
}
