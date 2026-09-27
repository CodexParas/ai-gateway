package com.codexparas.ai_gateway.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerationConfig(
    @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    @Positive @JsonProperty("top_k") Integer topK,
    @Positive @JsonProperty("max_output_tokens") Integer maxOutputTokens,
    List<String> stopSequences,
    @JsonProperty("response_mime_type") String responseMimeType,
    Object responseSchema) {}
