package com.codexparas.ai_gateway.execution;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

public record TransformedChatRequest(List<Message> messages, ChatOptions chatOptions) {
  public TransformedChatRequest {
    messages = List.copyOf(messages);
  }
}
