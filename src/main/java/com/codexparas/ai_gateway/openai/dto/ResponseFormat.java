package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseFormat(
    String type, String name, String description, Boolean strict, Object schema) {}
