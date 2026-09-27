package com.codexparas.ai_gateway.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicMessage(@NotBlank String role, @Valid List<AnthropicContentBlock> content) {}
