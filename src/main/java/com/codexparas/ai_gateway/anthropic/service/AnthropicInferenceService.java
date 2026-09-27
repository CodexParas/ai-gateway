package com.codexparas.ai_gateway.anthropic.service;

import com.codexparas.ai_gateway.anthropic.dto.AnthropicMessagesRequest;
import com.codexparas.ai_gateway.anthropic.dto.AnthropicMessagesResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AnthropicInferenceService {

  private final WebClient anthropicWebClient;

  public AnthropicInferenceService(WebClient anthropicWebClient) {
    this.anthropicWebClient = anthropicWebClient;
  }

  public Mono<AnthropicMessagesResponse> createMessage(AnthropicMessagesRequest request) {
    return anthropicWebClient
        .post()
        .uri("/v1/messages")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(AnthropicMessagesResponse.class);
  }

  public Flux<ServerSentEvent<String>> streamMessage(AnthropicMessagesRequest request) {
    return anthropicWebClient
        .post()
        .uri("/v1/messages")
        .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<>() {});
  }
}
