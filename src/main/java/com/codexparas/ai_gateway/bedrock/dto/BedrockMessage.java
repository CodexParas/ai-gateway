package com.codexparas.ai_gateway.bedrock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedrockMessage(
    @NotBlank String role, @NotEmpty List<@Valid BedrockContent> content) {}
