package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AzureEmbeddingsRequest(@NotEmpty List<String> input, String model) {}
