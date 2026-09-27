package com.codexparas.ai_gateway.openai.dto;

import tools.jackson.databind.JsonNode;

public record InferenceResponse(JsonNode response, String requestId) {}
