package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerateContentRequest(
    @NotEmpty List<@Valid GeminiContent> contents,
    @JsonProperty("system_instruction") @Valid GeminiContent systemInstruction,
    @JsonProperty("generation_config") @Valid GeminiGenerationConfig generationConfig,
    @JsonProperty("safety_settings") List<@Valid GeminiSafetySetting> safetySettings,
    List<Object> tools,
    @JsonProperty("cached_content") String cachedContent) {}
