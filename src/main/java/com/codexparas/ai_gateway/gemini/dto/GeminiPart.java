package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiPart(
    String text,
    @JsonProperty("inline_data") GeminiBlob inlineData,
    @JsonProperty("function_call") GeminiFunctionCall functionCall,
    @JsonProperty("function_response") GeminiFunctionResponse functionResponse) {
  public record GeminiBlob(
      @NotBlank @JsonProperty("mime_type") String mimeType, @NotBlank String data) {}

  public record GeminiFunctionCall(String name, Object args) {}

  public record GeminiFunctionResponse(String name, Object response) {}
}
