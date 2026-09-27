package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class OpenAiResponseTransformer extends AbstractResponseTransformer {

  public OpenAiResponseTransformer(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public String provider() {
    return "OPENAI";
  }

  @Override
  public ProviderResponse transform(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("id", responseId(response));
    root.put("object", "chat.completion");
    putNullable(root, "model", model(response));
    ArrayNode choices = root.putArray("choices");
    ObjectNode choice = choices.addObject();
    choice.put("index", 0);
    ObjectNode message = choice.putObject("message");
    putAssistantRole(message);
    message.put("content", text(response));
    putNullable(choice, "finish_reason", finishReason(response));
    putUsage(root, usage(response));
    return new ProviderResponse(null, root);
  }

  @Override
  public ProviderResponse transformChunk(GatewayRoute route, ChatResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("id", responseId(response));
    root.put("object", "chat.completion.chunk");
    putNullable(root, "model", model(response));
    ObjectNode choice = root.putArray("choices").addObject();
    choice.put("index", 0);
    ObjectNode delta = choice.putObject("delta");
    delta.put("content", text(response));
    putNullable(choice, "finish_reason", finishReason(response));
    return new ProviderResponse("data", root);
  }
}
