package com.codexparas.ai_gateway.execution;

public final class ProviderExecutionException extends RuntimeException {
  private final String provider;
  private final String model;

  public ProviderExecutionException(String provider, String model, Throwable cause) {
    super("Provider model execution failed", cause);
    this.provider = provider;
    this.model = model;
  }

  public String provider() {
    return provider;
  }

  public String model() {
    return model;
  }
}
