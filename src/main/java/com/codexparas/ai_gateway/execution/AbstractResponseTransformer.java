package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

abstract class AbstractResponseTransformer
    implements ResponseTransformer, StreamingResponseTransformer {

  protected final ObjectMapper objectMapper;

  protected AbstractResponseTransformer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public final boolean supports(GatewayRoute route) {
    return provider().equalsIgnoreCase(route.provider()) && !route.operation().equals("models");
  }

  protected static Generation generation(ChatResponse response) {
    return response.getResults().isEmpty() ? null : response.getResults().getFirst();
  }

  protected static String text(ChatResponse response) {
    Generation generation = generation(response);
    return generation == null || generation.getOutput() == null
        ? ""
        : generation.getOutput().getText();
  }

  protected static String responseId(ChatResponse response) {
    ChatResponseMetadata metadata = response.getMetadata();
    return metadata == null || metadata.getId() == null ? "" : metadata.getId();
  }

  protected static String model(ChatResponse response) {
    ChatResponseMetadata metadata = response.getMetadata();
    return metadata == null ? null : metadata.getModel();
  }

  protected static String finishReason(ChatResponse response) {
    Generation generation = generation(response);
    if (generation == null) return null;
    ChatGenerationMetadata metadata = generation.getMetadata();
    return metadata == null ? null : metadata.getFinishReason();
  }

  protected static Usage usage(ChatResponse response) {
    ChatResponseMetadata metadata = response.getMetadata();
    return metadata == null ? null : metadata.getUsage();
  }

  protected static void putNullable(ObjectNode node, String field, String value) {
    if (value == null) node.putNull(field);
    else node.put(field, value);
  }

  protected static void putUsage(ObjectNode node, Usage usage) {
    if (usage == null) return;
    putNullableInteger(node, "prompt_tokens", usage.getPromptTokens());
    putNullableInteger(node, "completion_tokens", usage.getCompletionTokens());
    putNullableInteger(node, "total_tokens", usage.getTotalTokens());
  }

  protected static void putNullableInteger(ObjectNode node, String field, Integer value) {
    if (value != null) node.put(field, value);
  }

  protected static void putAssistantRole(ObjectNode node) {
    node.put("role", "assistant");
  }

  protected static AssistantMessage assistant(ChatResponse response) {
    Generation generation = generation(response);
    return generation == null ? null : generation.getOutput();
  }
}
