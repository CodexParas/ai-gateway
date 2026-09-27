package com.codexparas.ai_gateway.model;

import java.util.List;
import tools.jackson.databind.JsonNode;

public interface ModelConfigurationValidator {

  List<String> validate(String requestedModel, JsonNode configuration);
}
