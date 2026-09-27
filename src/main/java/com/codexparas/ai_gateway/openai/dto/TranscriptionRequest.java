package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranscriptionRequest(
    @NotBlank String model,
    String language,
    String prompt,
    String responseFormat,
    Double temperature,
    @JsonProperty("timestamp_granularities[]") String timestampGranularities) {}
