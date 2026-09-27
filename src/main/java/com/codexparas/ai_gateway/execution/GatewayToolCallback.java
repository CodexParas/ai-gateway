package com.codexparas.ai_gateway.execution;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/** Carries an upstream tool schema without granting the gateway executable tool code. */
final class GatewayToolCallback implements ToolCallback {
  private final ToolDefinition definition;

  GatewayToolCallback(String name, String description, String inputSchema) {
    this.definition =
        DefaultToolDefinition.builder()
            .name(name)
            .description(description == null ? "" : description)
            .inputSchema(inputSchema == null || inputSchema.isBlank() ? "{}" : inputSchema)
            .build();
  }

  @Override
  public ToolDefinition getToolDefinition() {
    return definition;
  }

  @Override
  public String call(String arguments) {
    throw new UnsupportedOperationException(
        "Gateway tool callbacks are declarations only and cannot be executed");
  }
}
