package com.codexparas.ai_gateway.bedrock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedrockConverseRequest(
    @NotEmpty List<@Valid BedrockMessage> messages,
    @JsonProperty("system") String system,
    @JsonProperty("inference_config") @Valid BedrockInferenceConfig inferenceConfig,
    @JsonProperty("request_metadata") Map<String, String> requestMetadata) {}
