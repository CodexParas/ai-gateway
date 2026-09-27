package com.codexparas.ai_gateway.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicMessagesRequest(
    @NotBlank String model,
    @NotEmpty List<@Valid AnthropicMessage> messages,
    @Positive @JsonProperty("max_tokens") Integer maxTokens,
    String system,
    @JsonProperty("stop_sequences") List<String> stopSequences,
    Boolean stream,
    @DecimalMin("0.0") @DecimalMax("1.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    @JsonProperty("top_k") Integer topK,
    List<@Valid AnthropicTool> tools,
    @JsonProperty("tool_choice") @Valid AnthropicToolChoice toolChoice,
    Map<String, Object> metadata) {}
