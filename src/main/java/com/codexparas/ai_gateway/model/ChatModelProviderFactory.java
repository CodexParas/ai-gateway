package com.codexparas.ai_gateway.model;

import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.JsonNode;

/** Creates a Spring AI ChatModel for one configured gateway provider. */
public interface ChatModelProviderFactory {

  String provider();

  ChatModel create(JsonNode configuration);
}
