package com.codexparas.ai_gateway.model;

import com.codexparas.ai_gateway.guardrail.GuardrailFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class BasicModelConfigurationValidator implements ModelConfigurationValidator {

  private static final Set<String> PROVIDERS =
      Set.of("OPENAI", "ANTHROPIC", "BEDROCK", "GOOGLE", "AZURE");
  private static final Set<String> AUTH_TYPES =
      Set.of(
          "API_KEY",
          "OAUTH2",
          "AWS_CREDENTIALS",
          "ROLE",
          "WORKLOAD_IDENTITY",
          "AZURE_MANAGED_IDENTITY");

  private final GuardrailFactory guardrailFactory;

  public BasicModelConfigurationValidator(GuardrailFactory guardrailFactory) {
    this.guardrailFactory = guardrailFactory;
  }

  @Override
  public List<String> validate(String requestedModel, JsonNode configuration) {
    List<String> errors = new ArrayList<>();
    requireObject(configuration, errors);
    if (!errors.isEmpty()) {
      return errors;
    }

    requiredText(configuration, "model", errors);
    requiredText(configuration, "provider", errors);
    if (configuration.has("model")
        && configuration.get("model").isString()
        && !requestedModel.equals(configuration.get("model").asString())) {
      errors.add("model must match the Redis hash field");
    }
    if (configuration.has("provider") && configuration.get("provider").isString()) {
      String provider = configuration.get("provider").asString().toUpperCase(Locale.ROOT);
      if (!PROVIDERS.contains(provider)) {
        errors.add("provider must be one of " + Arrays.toString(PROVIDERS.toArray()));
      }
    }

    JsonNode auth = configuration.get("auth");
    if (auth == null || !auth.isObject()) {
      errors.add("auth must be an object");
    } else {
      requiredText(auth, "type", errors);
      if (auth.has("type") && auth.get("type").isString()) {
        String authType = auth.get("type").asString();
        if (!AUTH_TYPES.contains(authType)) {
          errors.add("auth.type is unsupported");
        } else if (authType.equals("API_KEY") && !auth.has("apiKey")) {
          errors.add("auth.apiKey is required for API_KEY authentication");
        } else if (authType.equals("ROLE") && !auth.has("roleArn")) {
          errors.add("auth.roleArn is required for ROLE authentication");
        }
      }
    }

    JsonNode request = configuration.get("request");
    if (request != null && !request.isObject()) {
      errors.add("request must be an object");
    }
    JsonNode guardrails = configuration.get("guardrails");
    errors.addAll(guardrailFactory.validateAll(guardrails));
    if (configuration.has("enabled")
        && (!configuration.get("enabled").isBoolean()
            || !configuration.get("enabled").asBoolean())) {
      errors.add("model configuration is disabled");
    }
    return errors;
  }

  private static void requireObject(JsonNode configuration, List<String> errors) {
    if (configuration == null || !configuration.isObject()) {
      errors.add("configuration must be a JSON object");
    }
  }

  private static void requiredText(JsonNode object, String field, List<String> errors) {
    requiredText(object, field, errors, field);
  }

  private static void requiredText(
      JsonNode object, String field, List<String> errors, String path) {
    if (!object.has(field)
        || !object.get(field).isString()
        || object.get(field).asString().isBlank()) {
      errors.add(path + " must be a non-blank string");
    }
  }
}
