package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiModel(
    String name,
    String displayName,
    String description,
    @JsonProperty("inputTokenLimit") Integer inputTokenLimit,
    @JsonProperty("outputTokenLimit") Integer outputTokenLimit) {}
