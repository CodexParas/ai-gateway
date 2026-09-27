package com.codexparas.ai_gateway.model.provider;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class AnthropicChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public AnthropicChatModelProviderFactory(ObjectProvider<AnthropicChatModel> models) {
    super("ANTHROPIC", models::getIfAvailable);
  }
}
