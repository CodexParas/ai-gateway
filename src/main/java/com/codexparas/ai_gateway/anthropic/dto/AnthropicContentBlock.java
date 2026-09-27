package com.codexparas.ai_gateway.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicContentBlock(
    @NotBlank String type,
    String text,
    Map<String, Object> source,
    String id,
    String name,
    Map<String, Object> input,
    Object content,
    @JsonProperty("is_error") Boolean isError) {}
