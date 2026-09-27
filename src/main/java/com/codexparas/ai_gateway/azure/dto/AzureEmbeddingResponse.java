package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureEmbeddingResponse(String object, List<Embedding> data, String model) {
  public record Embedding(String object, List<Double> embedding, Integer index) {}
}
