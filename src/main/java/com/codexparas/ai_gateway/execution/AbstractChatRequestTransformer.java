package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;

abstract class AbstractChatRequestTransformer implements RequestTransformer {

  @Override
  public final boolean supports(GatewayRoute route) {
    return provider().equalsIgnoreCase(route.provider())
        && !route.operation().equals("models")
        && !route.operation().startsWith("audio.");
  }

  @Override
  public final TransformedChatRequest transform(GatewayRoute route, JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new IllegalArgumentException("Request body must be a JSON object");
    }
    List<Message> messages = messages(body);
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("Request must contain at least one message");
    }
    return new TransformedChatRequest(messages, options(body));
  }

  private static List<Message> messages(JsonNode body) {
    List<Message> messages = new ArrayList<>();
    JsonNode system = body.get("system");
    if (system != null && system.isTextual()) {
      messages.add(new SystemMessage(system.asString()));
    }
    JsonNode systemInstruction = body.get("system_instruction");
    if (systemInstruction != null) {
      String content = content(systemInstruction);
      if (!content.isBlank()) messages.add(new SystemMessage(content));
    }

    JsonNode source = body.get("messages");
    if (source == null || !source.isArray()) source = body.get("contents");
    if (source == null || !source.isArray()) source = body.get("input");
    if (source != null && source.isArray()) {
      source.forEach(message -> addMessage(messages, message));
    } else if (body.path("input").isTextual()) {
      messages.add(new UserMessage(body.path("input").asString()));
    }
    return messages;
  }

  private static void addMessage(List<Message> messages, JsonNode node) {
    if (node == null || !node.isObject()) return;
    String role = node.path("role").asString("user").toLowerCase(Locale.ROOT);
    String text = content(node);
    switch (role) {
      case "system" -> messages.add(new SystemMessage(text));
      case "assistant", "model" -> messages.add(new AssistantMessage(text));
      case "tool" ->
          messages.add(
              ToolResponseMessage.builder()
                  .responses(
                      List.of(
                          new ToolResponseMessage.ToolResponse(
                              node.path("tool_call_id").asString(),
                              node.path("name").asString(),
                              text)))
                  .build());
      default -> messages.add(userMessage(node, text));
    }
  }

  private static UserMessage userMessage(JsonNode node, String text) {
    UserMessage.Builder builder = UserMessage.builder().text(text);
    JsonNode parts = node.get("content");
    if (parts != null && parts.isArray()) {
      List<Media> media = new ArrayList<>();
      parts.forEach(
          part -> {
            JsonNode image = part.get("image_url");
            if (image != null) {
              String url = image.isTextual() ? image.asString() : image.path("url").asString();
              if (!url.isBlank()) media.add(media(url, part.path("mime_type").asString("image/*")));
            }
            JsonNode uri = part.get("uri");
            if (uri != null && uri.isTextual()) {
              media.add(
                  media(
                      uri.asString(), part.path("mime_type").asString("application/octet-stream")));
            }
          });
      if (!media.isEmpty()) builder.media(media);
    }
    return builder.build();
  }

  private static Media media(String value, String mimeType) {
    return new Media(MimeTypeUtils.parseMimeType(mimeType), URI.create(value));
  }

  private static String content(JsonNode node) {
    JsonNode content = node.get("content");
    if (content == null) content = node.get("parts");
    if (content == null) return "";
    if (content.isTextual()) return content.asString();
    if (!content.isArray()) return content.toString();
    StringBuilder result = new StringBuilder();
    content.forEach(
        part -> {
          JsonNode text = part.isTextual() ? part : part.get("text");
          if (text != null && text.isTextual()) {
            if (!result.isEmpty()) result.append('\n');
            result.append(text.asString());
          }
        });
    return result.toString();
  }

  private ChatOptions options(JsonNode body) {
    boolean providerSpecificFormat =
        (provider().equalsIgnoreCase("OPENAI") || provider().equalsIgnoreCase("AZURE"))
            && body.path("response_format").isObject();
    ChatOptions.Builder<?> builder =
        providerSpecificFormat ? OpenAiChatOptions.builder() : ChatOptions.builder();
    setDouble(builder, "temperature", body.get("temperature"));
    setDouble(builder, "topP", first(body, "top_p", "topP"));
    setDouble(builder, "frequencyPenalty", body.get("frequency_penalty"));
    setDouble(builder, "presencePenalty", body.get("presence_penalty"));
    setInteger(builder, "maxTokens", first(body, "max_tokens", "max_completion_tokens"));
    setInteger(builder, "topK", body.get("top_k"));
    setResponseFormat(builder, body.get("response_format"));
    JsonNode stop = first(body, "stop", "stop_sequences");
    if (stop != null) {
      if (stop.isTextual()) builder.stopSequences(List.of(stop.asString()));
      if (stop.isArray()) {
        List<String> values = new ArrayList<>();
        stop.forEach(
            value -> {
              if (value.isTextual()) values.add(value.asString());
            });
        builder.stopSequences(values);
      }
    }
    return builder.build();
  }

  private static void setResponseFormat(ChatOptions.Builder<?> builder, JsonNode value) {
    if (!(builder instanceof OpenAiChatOptions.Builder openAi)
        || value == null
        || !value.isObject()) {
      return;
    }
    String type = value.path("type").asString();
    if (type.equalsIgnoreCase("text")) {
      openAi.responseFormat(
          OpenAiChatModel.ResponseFormat.builder()
              .type(OpenAiChatModel.ResponseFormat.Type.TEXT)
              .build());
    } else if (type.equalsIgnoreCase("json_object")) {
      openAi.responseFormat(
          OpenAiChatModel.ResponseFormat.builder()
              .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
              .build());
    } else if (type.equalsIgnoreCase("json_schema")) {
      JsonNode schema = value.path("json_schema").path("schema");
      openAi.responseFormat(
          OpenAiChatModel.ResponseFormat.builder()
              .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
              .jsonSchema(schema.isMissingNode() ? "{}" : schema.toString())
              .strict(value.path("json_schema").path("strict").asBoolean(false))
              .build());
    }
  }

  private static JsonNode first(JsonNode body, String first, String second) {
    JsonNode value = body.get(first);
    return value == null ? body.get(second) : value;
  }

  private static void setDouble(ChatOptions.Builder<?> builder, String field, JsonNode value) {
    if (value != null && value.isNumber()) {
      switch (field) {
        case "temperature" -> builder.temperature(value.asDouble());
        case "topP" -> builder.topP(value.asDouble());
        case "frequencyPenalty" -> builder.frequencyPenalty(value.asDouble());
        case "presencePenalty" -> builder.presencePenalty(value.asDouble());
        default -> {}
      }
    }
  }

  private static void setInteger(ChatOptions.Builder<?> builder, String field, JsonNode value) {
    if (value != null && value.isIntegralNumber()) {
      switch (field) {
        case "maxTokens" -> builder.maxTokens(value.asInt());
        case "topK" -> builder.topK(value.asInt());
        default -> {}
      }
    }
  }
}
