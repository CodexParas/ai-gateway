package com.codexparas.ai_gateway.azure.controller;

import com.codexparas.ai_gateway.azure.dto.AzureChatCompletionRequest;
import com.codexparas.ai_gateway.azure.dto.AzureChatCompletionResponse;
import com.codexparas.ai_gateway.azure.dto.AzureEmbeddingResponse;
import com.codexparas.ai_gateway.azure.dto.AzureEmbeddingsRequest;
import com.codexparas.ai_gateway.azure.service.AzureAiInferenceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/azure-ai")
public class AzureAiController {

  private final AzureAiInferenceService inferenceService;

  public AzureAiController(AzureAiInferenceService inferenceService) {
    this.inferenceService = inferenceService;
  }

  @PostMapping("/chat/completions")
  public Mono<AzureChatCompletionResponse> createChatCompletion(
      @Valid @RequestBody AzureChatCompletionRequest request) {
    return inferenceService.createChatCompletion(request);
  }

  @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> streamChatCompletion(
      @Valid @RequestBody AzureChatCompletionRequest request) {
    return inferenceService.streamChatCompletion(request);
  }

  @PostMapping("/embeddings")
  public Mono<AzureEmbeddingResponse> createEmbedding(
      @Valid @RequestBody AzureEmbeddingsRequest request) {
    return inferenceService.createEmbedding(request);
  }
}
