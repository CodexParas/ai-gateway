package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseTextConfig(@Valid ResponseFormat format) {}
