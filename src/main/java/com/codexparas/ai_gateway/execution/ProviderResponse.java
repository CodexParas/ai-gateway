package com.codexparas.ai_gateway.execution;

import tools.jackson.databind.JsonNode;

public record ProviderResponse(String eventName, JsonNode body) {}
