package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.model.ChatResponse;

public interface StreamingResponseTransformer {

  String provider();

  boolean supports(GatewayRoute route);

  ProviderResponse transformChunk(GatewayRoute route, ChatResponse response);
}
