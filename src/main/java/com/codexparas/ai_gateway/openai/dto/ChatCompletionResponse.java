package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
    String id, String object, Long created, String model, List<Choice> choices, Usage usage) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Choice(Integer index, ChatMessage message, String finishReason) {}
}
