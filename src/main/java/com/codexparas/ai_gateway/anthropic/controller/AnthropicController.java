package com.codexparas.ai_gateway.anthropic.controller;

import com.codexparas.ai_gateway.anthropic.dto.AnthropicMessagesRequest;
import com.codexparas.ai_gateway.anthropic.dto.AnthropicMessagesResponse;
import com.codexparas.ai_gateway.anthropic.service.AnthropicInferenceService;
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
@RequestMapping({"/api/v1/anthropic", "/v1"})
public class AnthropicController {

  private final AnthropicInferenceService inferenceService;

  public AnthropicController(AnthropicInferenceService inferenceService) {
    this.inferenceService = inferenceService;
  }

  @PostMapping("/messages")
  public Mono<AnthropicMessagesResponse> createMessage(
      @Valid @RequestBody AnthropicMessagesRequest request) {
    return inferenceService.createMessage(request);
  }

  @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> streamMessage(
      @Valid @RequestBody AnthropicMessagesRequest request) {
    return inferenceService.streamMessage(request);
  }
}
