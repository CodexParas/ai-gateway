package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public final class GoogleResponseTransformer extends AbstractResponseTransformer {

  public GoogleResponseTransformer(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public String provider() {
    return "GOOGLE";
  }

  @Override
  public ProviderResponse transform(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode candidate = root.putArray("candidates").addObject();
    ObjectNode content = candidate.putObject("content");
    content.put("role", "model");
    content.putArray("parts").addObject().put("text", text(response));
    putNullable(candidate, "finishReason", finishReason(response));
    addUsage(root, response);
    return new ProviderResponse(null, root);
  }

  @Override
  public ProviderResponse transformChunk(GatewayRoute route, ChatResponse response) {
    return new ProviderResponse(null, transform(route, response).body());
  }

  private void addUsage(ObjectNode root, ChatResponse response) {
    if (usage(response) == null) return;
    ObjectNode usage = root.putObject("usageMetadata");
    putNullableInteger(usage, "promptTokenCount", usage(response).getPromptTokens());
    putNullableInteger(usage, "candidatesTokenCount", usage(response).getCompletionTokens());
    putNullableInteger(usage, "totalTokenCount", usage(response).getTotalTokens());
  }
}
