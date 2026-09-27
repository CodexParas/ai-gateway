package com.codexparas.ai_gateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.execution.GatewayResponseFilter;
import com.codexparas.ai_gateway.execution.RequestExecutionFilter;
import com.codexparas.ai_gateway.route.GatewayRouteFilter;
import com.codexparas.ai_gateway.route.validation.ProviderRequestValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

class GatewayFilterOrderingTest {
  @Test
  void preservesTheRequiredPipelineOrder() {
    assertThat(order(RequestLoggingFilter.class)).isLessThan(order(GatewayExceptionFilter.class));
    assertThat(order(GatewayExceptionFilter.class)).isLessThan(order(GatewayRouteFilter.class));
    assertThat(order(GatewayRouteFilter.class))
        .isLessThan(order(ProviderRequestValidationFilter.class));
    assertThat(order(ProviderRequestValidationFilter.class))
        .isLessThan(order(ModelResolutionFilter.class));
    assertThat(order(ModelResolutionFilter.class)).isLessThan(order(ChatModelFilter.class));
    assertThat(order(ChatModelFilter.class)).isLessThan(order(RequestExecutionFilter.class));
    assertThat(order(RequestExecutionFilter.class))
        .isLessThan(order(ResponseGuardrailFilter.class));
    assertThat(order(ResponseGuardrailFilter.class)).isLessThan(order(GatewayResponseFilter.class));
  }

  private static int order(Class<?> type) {
    return type.getAnnotation(Order.class).value();
  }
}
