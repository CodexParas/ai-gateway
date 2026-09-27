package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponsesRequest(
    @NotBlank String model,
    String input,
    String instructions,
    List<@Valid ChatTool> tools,
    @Valid ResponseTextConfig text,
    @JsonProperty("max_output_tokens") @Positive Integer maxOutputTokens,
    @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
    @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("top_p") Double topP,
    Boolean stream,
    Boolean store,
    @JsonProperty("previous_response_id") String previousResponseId,
    Map<String, String> metadata) {}
