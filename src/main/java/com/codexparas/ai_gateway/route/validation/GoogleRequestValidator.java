package com.codexparas.ai_gateway.route.validation;

import org.springframework.stereotype.Component;

@Component
public final class GoogleRequestValidator extends AbstractProviderRequestValidator {
  @Override
  public String provider() {
    return "GOOGLE";
  }
}
