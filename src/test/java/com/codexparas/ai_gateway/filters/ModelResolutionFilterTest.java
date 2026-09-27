package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codexparas.ai_gateway.model.ModelConfigurationService;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ModelResolutionFilterTest {

  @Test
  void extractsBodyModelAndLoadsValidatedConfiguration() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ModelConfigurationService service = mock(ModelConfigurationService.class);
    JsonNode configuration = objectMapper.readTree("{\"provider\":\"OPENAI\",\"model\":\"gpt-5\"}");
    when(service.findAndValidate("gpt-5")).thenReturn(Mono.just(configuration));
    ModelResolutionFilter filter = new ModelResolutionFilter(service, objectMapper);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.setAttribute(
        GatewayRequestAttributes.ROUTE, new GatewayRoute("OPENAI", "chat.completions", false));
    request.setAttribute(
        GatewayRequestAttributes.REQUEST_BODY, "{\"model\":\"gpt-5\",\"messages\":[]}");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<HttpServletRequest> continuedRequest = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (nextRequest, ignoredResponse) -> continuedRequest.set((HttpServletRequest) nextRequest));

    assertThat(continuedRequest.get().getAttribute(GatewayRequestAttributes.MODEL_NAME))
        .isEqualTo("gpt-5");
    assertThat(continuedRequest.get().getAttribute(GatewayRequestAttributes.MODEL_CONFIGURATION))
        .isEqualTo(configuration);
  }
}
