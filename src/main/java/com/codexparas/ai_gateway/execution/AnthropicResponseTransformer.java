package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public final class AnthropicResponseTransformer extends AbstractResponseTransformer {

  public AnthropicResponseTransformer(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public String provider() {
    return "ANTHROPIC";
  }

  @Override
  public ProviderResponse transform(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("id", responseId(response));
    root.put("type", "message");
    root.put("role", "assistant");
    var content = root.putArray("content");
    if (!text(response).isBlank())
      content.addObject().put("type", "text").put("text", text(response));
    for (var call : toolCalls(response)) {
      ObjectNode tool = content.addObject();
      tool.put("type", "tool_use");
      putNullable(tool, "id", call.id());
      putNullable(tool, "name", call.name());
      try {
        tool.set("input", objectMapper.readTree(call.arguments()));
      } catch (Exception exception) {
        tool.putObject("input");
      }
    }
    putNullable(root, "model", model(response));
    putNullable(root, "stop_reason", finishReason(response));
    putUsage(root, usage(response));
    return new ProviderResponse(null, root);
  }

  @Override
  public ProviderResponse transformChunk(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "content_block_delta");
    ObjectNode delta = root.putObject("delta");
    delta.put("type", "text_delta");
    delta.put("text", text(response));
    return new ProviderResponse("content_block_delta", root);
  }
}
