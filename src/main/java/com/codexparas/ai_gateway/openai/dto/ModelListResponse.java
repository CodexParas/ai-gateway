package com.codexparas.ai_gateway.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelListResponse(String object, List<ModelResponse> data) {}
