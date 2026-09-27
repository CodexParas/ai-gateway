package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(@NotBlank String role, @NotBlank String content, String name) {}
