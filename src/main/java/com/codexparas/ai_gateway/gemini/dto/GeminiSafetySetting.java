package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record GeminiSafetySetting(
    @NotBlank String category, @NotBlank @JsonProperty("threshold") String threshold) {}
