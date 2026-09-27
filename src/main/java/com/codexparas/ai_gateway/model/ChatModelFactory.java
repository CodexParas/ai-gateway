package com.codexparas.ai_gateway.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Resolves provider-specific factories and prepares a Spring AI ChatModel. */
@Component
public class ChatModelFactory {

  private final Map<String, ChatModelProviderFactory> providerFactories;

  public ChatModelFactory(List<ChatModelProviderFactory> providerFactories) {
    this.providerFactories =
        providerFactories.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    factory -> factory.provider().toUpperCase(Locale.ROOT), Function.identity()));
  }

  public PreparedChatModel prepare(String requestedModel, JsonNode configuration) {
    String provider = requiredText(configuration, "provider").toUpperCase(Locale.ROOT);
    ChatModelProviderFactory providerFactory = providerFactories.get(provider);
    if (providerFactory == null) {
      throw new ChatModelPreparationException(
          "No Spring AI ChatModel factory registered for provider " + provider);
    }
    ProviderModelConfiguration modelConfiguration =
        new ProviderModelConfiguration(
            provider,
            requiredText(configuration, "model"),
            nullableText(configuration, "url"),
            nullableText(configuration, "region"),
            nullableText(configuration, "deployment"),
            nullableText(configuration, "apiVersion"),
            configuration.get("auth"),
            configuration.get("request"),
            configuration.get("guardrails"));
    ChatModel chatModel = providerFactory.create(modelConfiguration);
    if (chatModel == null) {
      throw new ChatModelPreparationException(
          "Spring AI ChatModel factory returned no model for provider " + provider);
    }
    return new PreparedChatModel(
        chatModel,
        requestedModel,
        provider,
        modelConfiguration.model(),
        modelConfiguration.url(),
        modelConfiguration.region(),
        modelConfiguration.deployment(),
        modelConfiguration.apiVersion(),
        modelConfiguration.auth(),
        modelConfiguration.request(),
        modelConfiguration.guardrails());
  }

  private static String requiredText(JsonNode object, String field) {
    JsonNode value = object.get(field);
    if (value == null || value.isNull() || value.asString().isBlank()) {
      throw new ChatModelPreparationException(
          "Missing required model configuration field: " + field);
    }
    return value.asString();
  }

  private static String nullableText(JsonNode object, String field) {
    JsonNode value = object.get(field);
    return value == null || value.isNull() ? null : value.asString();
  }

  public static final class ChatModelPreparationException extends RuntimeException {
    public ChatModelPreparationException(String message) {
      super(message);
    }
  }
}
