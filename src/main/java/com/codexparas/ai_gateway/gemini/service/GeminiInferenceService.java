package com.codexparas.ai_gateway.gemini.service;

import com.codexparas.ai_gateway.config.GeminiProperties;
import com.codexparas.ai_gateway.gemini.dto.GeminiGenerateContentRequest;
import com.codexparas.ai_gateway.gemini.dto.GeminiGenerateContentResponse;
import com.codexparas.ai_gateway.gemini.dto.GeminiModel;
import com.codexparas.ai_gateway.gemini.dto.GeminiModelList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GeminiInferenceService {

  private final WebClient geminiWebClient;
  private final GeminiProperties properties;

  public GeminiInferenceService(
      @Qualifier("geminiWebClient") WebClient geminiWebClient, GeminiProperties properties) {
    this.geminiWebClient = geminiWebClient;
    this.properties = properties;
  }

  private static String normalizeModel(String model) {
    return model.startsWith("models/") ? model.substring("models/".length()) : model;
  }

  public Mono<GeminiGenerateContentResponse> generateContent(
      String model, GeminiGenerateContentRequest request) {
    return geminiWebClient
        .post()
        .uri("/models/{model}:generateContent", normalizeModel(model))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(GeminiGenerateContentResponse.class);
  }

  public Flux<ServerSentEvent<String>> streamGenerateContent(
      String model, GeminiGenerateContentRequest request) {
    return geminiWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/models/{model}:streamGenerateContent")
                    .queryParam("alt", "sse")
                    .build(normalizeModel(model)))
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<>() {});
  }

  public Mono<GeminiModelList> listModels() {
    return geminiWebClient.get().uri("/models").retrieve().bodyToMono(GeminiModelList.class);
  }

  public Mono<GeminiModel> getModel(String model) {
    return geminiWebClient
        .get()
        .uri("/models/{model}", normalizeModel(model))
        .retrieve()
        .bodyToMono(GeminiModel.class);
  }
}
