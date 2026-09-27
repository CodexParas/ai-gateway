package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ChatModelFactory;
import com.codexparas.ai_gateway.model.ChatModelProviderFactory;
import java.util.function.Supplier;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.JsonNode;

/** Common adapter for provider factories backed by Spring AI auto-configured models. */
abstract class AbstractSpringAiChatModelProviderFactory implements ChatModelProviderFactory {

  private final String provider;
  private final Supplier<? extends ChatModel> modelSupplier;

  protected AbstractSpringAiChatModelProviderFactory(
      String provider, Supplier<? extends ChatModel> modelSupplier) {
    this.provider = provider;
    this.modelSupplier = modelSupplier;
  }

  @Override
  public final String provider() {
    return provider;
  }

  @Override
  public final ChatModel create(JsonNode configuration) {
    ChatModel model = modelSupplier.get();
    if (model == null) {
      throw new ChatModelFactory.ChatModelPreparationException(
          "Spring AI " + provider + " ChatModel is not configured");
    }
    return model;
  }
}
