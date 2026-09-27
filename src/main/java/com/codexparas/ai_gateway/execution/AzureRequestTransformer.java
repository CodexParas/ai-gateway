package com.codexparas.ai_gateway.execution;

import org.springframework.stereotype.Component;

@Component
public final class AzureRequestTransformer extends AbstractChatRequestTransformer {
  @Override
  public String provider() {
    return "AZURE";
  }
}
