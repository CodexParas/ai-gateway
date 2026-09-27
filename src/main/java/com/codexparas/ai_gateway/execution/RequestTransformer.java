package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import tools.jackson.databind.JsonNode;

public interface RequestTransformer {

  String provider();

  boolean supports(GatewayRoute route);

  TransformedChatRequest transform(GatewayRoute route, JsonNode requestBody);
}
