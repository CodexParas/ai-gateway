package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureChatCompletionResponse(
    String id, String object, Long created, String model, List<Choice> choices, Usage usage) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Choice(
      Integer index, AzureMessage message, @JsonProperty("finish_reason") String finishReason) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Usage(
      @JsonProperty("prompt_tokens") Integer promptTokens,
      @JsonProperty("completion_tokens") Integer completionTokens,
      @JsonProperty("total_tokens") Integer totalTokens) {}
}
