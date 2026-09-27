package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionsRequest(
    @NotBlank String model,
    @NotEmpty List<@Valid ChatMessage> messages,
    List<@Valid ChatTool> tools,
    @JsonProperty("tool_choice") String toolChoice,
    @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    @JsonProperty("max_completion_tokens") @Positive Integer maxCompletionTokens,
    Boolean stream,
    @JsonProperty("response_format") @Valid ResponseFormat responseFormat,
    String user) {}
