package com.codexparas.ai_gateway.model.provider;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class OpenAiChatModelProviderFactory extends AbstractSpringAiChatModelProviderFactory {

  public OpenAiChatModelProviderFactory(ObjectProvider<OpenAiChatModel> models) {
    super("OPENAI", models::getIfAvailable);
  }
}
