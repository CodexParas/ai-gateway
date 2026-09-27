package com.codexparas.ai_gateway.execution;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class AzureResponseTransformer extends OpenAiCompatibleResponseTransformer {
  public AzureResponseTransformer(ObjectMapper objectMapper) {
    super(objectMapper, "AZURE");
  }
}
