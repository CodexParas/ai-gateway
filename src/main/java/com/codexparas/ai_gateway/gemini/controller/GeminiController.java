package com.codexparas.ai_gateway.gemini.controller;

import com.codexparas.ai_gateway.gemini.dto.GeminiGenerateContentRequest;
import com.codexparas.ai_gateway.gemini.dto.GeminiGenerateContentResponse;
import com.codexparas.ai_gateway.gemini.dto.GeminiModel;
import com.codexparas.ai_gateway.gemini.dto.GeminiModelList;
import com.codexparas.ai_gateway.gemini.service.GeminiInferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@Deprecated(forRemoval = false)
@RequestMapping({"/api/v1/gemini", "/v1beta"})
public class GeminiController {

  private final GeminiInferenceService inferenceService;

  public GeminiController(GeminiInferenceService inferenceService) {
    this.inferenceService = inferenceService;
  }

  @PostMapping("/models/{model}:generateContent")
  public Mono<GeminiGenerateContentResponse> generateContent(
      @PathVariable @NotBlank String model,
      @Valid @RequestBody GeminiGenerateContentRequest request) {
    return inferenceService.generateContent(model, request);
  }

  @PostMapping(
      value = "/models/{model}:streamGenerateContent",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> streamGenerateContent(
      @PathVariable @NotBlank String model,
      @Valid @RequestBody GeminiGenerateContentRequest request) {
    return inferenceService.streamGenerateContent(model, request);
  }

  @GetMapping("/models")
  public Mono<GeminiModelList> listModels() {
    return inferenceService.listModels();
  }

  @GetMapping("/models/{model}")
  public Mono<GeminiModel> getModel(@PathVariable @NotBlank String model) {
    return inferenceService.getModel(model);
  }
}
