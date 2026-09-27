package com.codexparas.ai_gateway.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicMessagesResponse(
    String id,
    String type,
    String role,
    List<AnthropicContentBlock> content,
    String model,
    @JsonProperty("stop_reason") String stopReason,
    @JsonProperty("stop_sequence") String stopSequence,
    Map<String, Object> usage) {}
