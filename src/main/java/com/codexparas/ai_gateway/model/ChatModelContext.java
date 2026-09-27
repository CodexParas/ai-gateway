package com.codexparas.ai_gateway.model;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import reactor.util.context.ContextView;

/** Request-scoped access to the prepared model for gateway execution components. */
public final class ChatModelContext {

  public static final String REACTOR_CONTEXT_KEY = ChatModelContext.class.getName();
  public static final String REQUEST_ATTRIBUTE = ChatModelContext.class.getName();

  private static final ThreadLocal<ChatModel> CURRENT = new ThreadLocal<>();

  private ChatModelContext() {}

  public static void set(ChatModel model) {
    CURRENT.set(model);
  }

  public static void clear() {
    CURRENT.remove();
  }

  public static Optional<ChatModel> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static ChatModel require() {
    return current()
        .orElseThrow(
            () -> new NoSuchElementException("No ChatModel is bound to the current thread"));
  }

  public static ChatModel from(ContextView contextView) {
    return contextView.getOrDefault(REACTOR_CONTEXT_KEY, require());
  }
}
