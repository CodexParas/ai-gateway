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
    if (toolCalls(response).isEmpty()) message.put("content", text(response));
    else message.putNull("content");
    addToolCalls(message, response);
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
    if (!text(response).isBlank()) delta.put("content", text(response));
    addToolCalls(delta, response);
    putNullable(choice, "finish_reason", finishReason(response));
    return new ProviderResponse("data", root);
  }

  private static void addToolCalls(ObjectNode node, ChatResponse response) {
    if (toolCalls(response).isEmpty()) return;
    var calls = node.putArray("tool_calls");
    int index = 0;
    for (var call : toolCalls(response)) {
      ObjectNode item = calls.addObject();
      item.put("index", index++);
      putNullable(item, "id", call.id());
      item.put("type", call.type() == null || call.type().isBlank() ? "function" : call.type());
      ObjectNode function = item.putObject("function");
      putNullable(function, "name", call.name());
      putNullable(function, "arguments", call.arguments());
    }
  }
}
