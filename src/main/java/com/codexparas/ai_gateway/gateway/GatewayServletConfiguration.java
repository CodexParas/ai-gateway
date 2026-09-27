package com.codexparas.ai_gateway.gateway;

import jakarta.servlet.Servlet;
import java.util.List;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** Registers the controller-free terminal entry point for canonical provider routes. */
@Configuration
public class GatewayServletConfiguration {

  @Bean
  ServletRegistrationBean<Servlet> gatewayFallbackServlet(ObjectMapper objectMapper) {
    ServletRegistrationBean<Servlet> registration =
        new ServletRegistrationBean<>(new GatewayFallbackServlet(objectMapper), providerMappings());
    registration.setName("gatewayFallbackServlet");
    registration.setLoadOnStartup(1);
    return registration;
  }

  private static String[] providerMappings() {
    return List.of("/openai/*", "/anthropic/*", "/bedrock/*", "/google/*", "/azure/*")
        .toArray(String[]::new);
  }
}
