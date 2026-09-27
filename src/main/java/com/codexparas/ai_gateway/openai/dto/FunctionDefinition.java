package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FunctionDefinition(
    @NotBlank String name, String description, Map<String, Object> parameters, Boolean strict) {}
