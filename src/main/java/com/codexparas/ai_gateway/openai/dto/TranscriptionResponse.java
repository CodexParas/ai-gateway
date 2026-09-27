package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptionResponse(
    String text, String language, Double duration, Object segments) {}
