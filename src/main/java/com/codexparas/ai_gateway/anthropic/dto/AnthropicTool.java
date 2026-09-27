package com.codexparas.ai_gateway.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicTool(
    @NotBlank String name,
    String description,
    @JsonProperty("input_schema") Map<String, Object> inputSchema) {}
