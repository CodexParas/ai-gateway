package com.codexparas.ai_gateway.model;

import org.springframework.ai.chat.model.ChatModel;

/** Creates a Spring AI ChatModel for one configured gateway provider. */
public interface ChatModelProviderFactory {

  String provider();

  ChatModel create(ProviderModelConfiguration configuration);
}
