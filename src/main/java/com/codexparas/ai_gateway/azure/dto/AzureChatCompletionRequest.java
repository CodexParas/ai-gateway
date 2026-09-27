package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AzureChatCompletionRequest(
    @NotEmpty List<@Valid AzureMessage> messages,
    @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    @Positive @JsonProperty("max_tokens") Integer maxTokens,
    Boolean stream,
    @JsonProperty("response_format") AzureResponseFormat responseFormat,
    String model) {}
