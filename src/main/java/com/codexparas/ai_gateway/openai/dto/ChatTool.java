package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatTool(@NotNull String type, @Valid FunctionDefinition function) {}
