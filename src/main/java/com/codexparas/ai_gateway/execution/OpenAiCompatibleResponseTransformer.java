package com.codexparas.ai_gateway.execution;

import tools.jackson.databind.ObjectMapper;

/** Shared OpenAI response shape for OpenAI-compatible providers. */
abstract class OpenAiCompatibleResponseTransformer extends OpenAiResponseTransformer {
  private final String provider;

  protected OpenAiCompatibleResponseTransformer(ObjectMapper objectMapper, String provider) {
    super(objectMapper);
    this.provider = provider;
  }

  @Override
  public String provider() {
    return provider;
  }
}
