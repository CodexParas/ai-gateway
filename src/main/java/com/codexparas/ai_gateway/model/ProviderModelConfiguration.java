package com.codexparas.ai_gateway.model;

import tools.jackson.databind.JsonNode;

/** Typed view of a model configuration resolved from the gateway store. */
public record ProviderModelConfiguration(
    String provider,
    String model,
    String url,
    String region,
    String deployment,
    String apiVersion,
    JsonNode auth,
    JsonNode request,
    JsonNode guardrails) {

  public String authValue(String... names) {
    return text(auth, names);
  }

  public String requestValue(String... names) {
    return text(request, names);
  }

  private static String text(JsonNode object, String... names) {
    if (object == null || !object.isObject()) return null;
    for (String name : names) {
      JsonNode value = object.get(name);
      if (value != null && value.isValueNode() && !value.asString().isBlank()) {
        return value.asString();
      }
    }
    return null;
  }
}
