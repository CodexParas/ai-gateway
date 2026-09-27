package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiContent(String role, @NotEmpty List<@Valid GeminiPart> parts) {}
