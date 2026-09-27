package com.codexparas.ai_gateway.azure.service;

import com.codexparas.ai_gateway.azure.dto.AzureChatCompletionRequest;
import com.codexparas.ai_gateway.azure.dto.AzureChatCompletionResponse;
import com.codexparas.ai_gateway.azure.dto.AzureEmbeddingResponse;
import com.codexparas.ai_gateway.azure.dto.AzureEmbeddingsRequest;
import com.codexparas.ai_gateway.config.AzureAiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AzureAiInferenceService {

  private final WebClient azureAiWebClient;
  private final AzureAiProperties properties;

  public AzureAiInferenceService(
      @Qualifier("azureAiWebClient") WebClient azureAiWebClient, AzureAiProperties properties) {
    this.azureAiWebClient = azureAiWebClient;
    this.properties = properties;
  }

  public Mono<AzureChatCompletionResponse> createChatCompletion(
      AzureChatCompletionRequest request) {
    return azureAiWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/chat/completions")
                    .queryParam("api-version", properties.apiVersion())
                    .build())
        .bodyValue(request)
        .retrieve()
        .bodyToMono(AzureChatCompletionResponse.class);
  }

  public Flux<ServerSentEvent<String>> streamChatCompletion(AzureChatCompletionRequest request) {
    return azureAiWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/chat/completions")
                    .queryParam("api-version", properties.apiVersion())
                    .build())
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<>() {});
  }

  public Mono<AzureEmbeddingResponse> createEmbedding(AzureEmbeddingsRequest request) {
    return azureAiWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/embeddings")
                    .queryParam("api-version", properties.apiVersion())
                    .build())
        .bodyValue(request)
        .retrieve()
        .bodyToMono(AzureEmbeddingResponse.class);
  }
}
