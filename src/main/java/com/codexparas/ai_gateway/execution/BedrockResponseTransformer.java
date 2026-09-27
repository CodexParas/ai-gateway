package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public final class BedrockResponseTransformer extends AbstractResponseTransformer {

  public BedrockResponseTransformer(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public String provider() {
    return "BEDROCK";
  }

  @Override
  public ProviderResponse transform(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode output = root.putObject("output");
    ObjectNode message = output.putObject("message");
    message.put("role", "assistant");
    message.putArray("content").addObject().put("text", text(response));
    putNullable(root, "stopReason", finishReason(response));
    if (usage(response) != null) {
      ObjectNode usage = root.putObject("usage");
      putNullableInteger(usage, "inputTokens", usage(response).getPromptTokens());
      putNullableInteger(usage, "outputTokens", usage(response).getCompletionTokens());
      putNullableInteger(usage, "totalTokens", usage(response).getTotalTokens());
    }
    return new ProviderResponse(null, root);
  }

  @Override
  public ProviderResponse transformChunk(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.putObject("contentBlockDelta").putObject("delta").put("text", text(response));
    return new ProviderResponse("contentBlockDelta", root);
  }
}
