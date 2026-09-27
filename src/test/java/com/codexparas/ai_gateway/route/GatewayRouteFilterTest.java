package com.codexparas.ai_gateway.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class GatewayRouteFilterTest {

  private final GatewayRouteFilter filter = new GatewayRouteFilter(new ObjectMapper());

  @Test
  void resolvesProviderAndOperationFromCanonicalPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/anthropic/messages");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<GatewayRoute> route = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (ignoredRequest, ignoredResponse) ->
            route.set((GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE)));

    assertThat(route.get()).isEqualTo(new GatewayRoute("ANTHROPIC", "messages", false));
  }

  @Test
  void detectsStreamingFromAcceptHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/chat/completions");
    request.addHeader("Accept", "text/event-stream");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<GatewayRoute> route = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (ignoredRequest, ignoredResponse) ->
            route.set((GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE)));

    assertThat(route.get().streaming()).isTrue();
  }

  @Test
  void rejectsUnknownProviderOperation() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openai/unknown");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getContentAsString()).contains("unsupported_route");
  }
}
