package com.codexparas.ai_gateway.bedrock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedrockContent(@NotBlank String text) {}
