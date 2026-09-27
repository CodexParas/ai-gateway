package com.codexparas.ai_gateway.bedrock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedrockConverseResponse(
    String modelId,
    String role,
    String text,
    String stopReason,
    Integer inputTokens,
    Integer outputTokens,
    Integer totalTokens) {}
