package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ProviderModelConfiguration;
import com.google.genai.Client;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GoogleGenAiChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public GoogleGenAiChatModelProviderFactory(ObjectProvider<GoogleGenAiChatModel> models) {
    super("GOOGLE", models::getIfAvailable);
  }

  @Override
  public GoogleGenAiChatModel create(ProviderModelConfiguration configuration) {
    Client.Builder client = Client.builder();
    String apiKey = configuration.authValue("apiKey", "api_key", "token");
    if (apiKey != null) client.apiKey(apiKey);
    String project = configuration.authValue("project", "projectId", "project_id");
    if (project != null) client.project(project);
    String location = configuration.region();
    if (location != null) client.location(location).vertexAI(true);
    GoogleGenAiChatOptions options =
        GoogleGenAiChatOptions.builder().model(configuration.model()).build();
    return GoogleGenAiChatModel.builder().genAiClient(client.build()).options(options).build();
  }
}
