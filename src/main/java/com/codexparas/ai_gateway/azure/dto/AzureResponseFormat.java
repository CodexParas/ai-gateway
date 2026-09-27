package com.codexparas.ai_gateway.azure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AzureResponseFormat(String type) {}
