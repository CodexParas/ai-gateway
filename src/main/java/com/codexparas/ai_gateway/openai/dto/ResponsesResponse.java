package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponsesResponse(
    String id,
    String object,
    String status,
    String model,
    List<ResponseOutput> output,
    Usage usage) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResponseOutput(
      String id, String type, String status, String role, Object content) {}
}
