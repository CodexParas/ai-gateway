package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Usage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {}
