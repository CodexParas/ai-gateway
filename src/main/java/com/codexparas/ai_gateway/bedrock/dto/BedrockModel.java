package com.codexparas.ai_gateway.bedrock.dto;

import java.util.List;

public record BedrockModel(
    String modelId,
    String modelName,
    String providerName,
    List<String> inputModalities,
    List<String> outputModalities,
    Boolean responseStreamingSupported) {}
