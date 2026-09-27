package com.codexparas.ai_gateway.model.provider;

import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GoogleGenAiChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public GoogleGenAiChatModelProviderFactory(ObjectProvider<GoogleGenAiChatModel> models) {
    super("GOOGLE", models::getIfAvailable);
  }
}
