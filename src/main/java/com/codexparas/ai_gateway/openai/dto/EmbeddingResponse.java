package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingResponse(String object, List<Embedding> data, String model, Usage usage) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Embedding(String object, List<Double> embedding, Integer index) {}
}
