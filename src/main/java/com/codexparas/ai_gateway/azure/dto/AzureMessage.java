package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AzureMessage(@NotBlank String role, @NotBlank String content, String name) {}
