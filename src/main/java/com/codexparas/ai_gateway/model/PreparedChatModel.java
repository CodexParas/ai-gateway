package com.codexparas.ai_gateway.model;

import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.JsonNode;

/** Request-scoped Spring AI model together with gateway metadata. */
public record PreparedChatModel(
    ChatModel chatModel,
    String requestedModel,
    String provider,
    String model,
    String url,
    String region,
    String deployment,
    String apiVersion,
    JsonNode auth,
    JsonNode request,
    JsonNode guardrails) {}
