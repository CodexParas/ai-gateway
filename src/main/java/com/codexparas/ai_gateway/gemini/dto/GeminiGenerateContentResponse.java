package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateContentResponse(
    List<Candidate> candidates,
    @JsonProperty("prompt_feedback") Map<String, Object> promptFeedback,
    @JsonProperty("usage_metadata") UsageMetadata usageMetadata) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Candidate(
      GeminiContent content,
      @JsonProperty("finish_reason") String finishReason,
      @JsonProperty("safety_ratings") List<Map<String, Object>> safetyRatings) {}

  public record UsageMetadata(
      @JsonProperty("prompt_token_count") Integer promptTokenCount,
      @JsonProperty("candidates_token_count") Integer candidatesTokenCount,
      @JsonProperty("total_token_count") Integer totalTokenCount) {}
}
