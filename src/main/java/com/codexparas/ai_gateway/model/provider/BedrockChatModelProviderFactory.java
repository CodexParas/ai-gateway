package com.codexparas.ai_gateway.model.provider;

import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class BedrockChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public BedrockChatModelProviderFactory(ObjectProvider<BedrockProxyChatModel> models) {
    super("BEDROCK", models::getIfAvailable);
  }
}
