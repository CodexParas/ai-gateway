package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import tools.jackson.databind.JsonNode;

public interface ProviderRequestValidator {

  String provider();

  List<String> validate(GatewayRoute route, JsonNode body);
}
