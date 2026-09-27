package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpeechRequest(
    @NotBlank String model,
    @NotBlank String input,
    @NotBlank String voice,
    String instructions,
    String responseFormat,
    @DecimalMin("0.25") @DecimalMax("4.0") Double speed,
    @JsonProperty("stream_format") String streamFormat) {}
