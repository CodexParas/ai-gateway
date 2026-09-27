package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.model.ChatModelContext;
import com.codexparas.ai_gateway.model.ChatModelFactory;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;

@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@Component
public class ChatModelFilter extends OncePerRequestFilter {
  private final ChatModelFactory chatModelFactory;

  public ChatModelFilter(ChatModelFactory chatModelFactory) {
    this.chatModelFactory = chatModelFactory;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    String model = (String) request.getAttribute(GatewayRequestAttributes.MODEL_NAME);
    JsonNode configuration =
        (JsonNode) request.getAttribute(GatewayRequestAttributes.MODEL_CONFIGURATION);
    if (model == null || configuration == null) {
      chain.doFilter(request, response);
      return;
    }
    PreparedChatModel prepared = chatModelFactory.prepare(model, configuration);
    request.setAttribute(GatewayRequestAttributes.CHAT_MODEL, prepared.chatModel());
    request.setAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL, prepared);
    ChatModelContext.set(prepared.chatModel());
    try {
      chain.doFilter(request, response);
    } finally {
      ChatModelContext.clear();
    }
  }
}
