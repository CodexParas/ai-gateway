package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmbeddingsRequest(
    @NotBlank String model,
    @NotEmpty List<@NotBlank String> input,
    @JsonProperty("encoding_format") String encodingFormat,
    @Positive Integer dimensions,
    String user) {}
