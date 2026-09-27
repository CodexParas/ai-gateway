package com.codexparas.ai_gateway.bedrock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedrockInferenceConfig(
    @Positive @JsonProperty("max_tokens") Integer maxTokens,
    @DecimalMin("0.0") @DecimalMax("1.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    List<String> stopSequences) {}
