package com.codexparas.ai_gateway.openai.controller;

import com.codexparas.ai_gateway.openai.dto.*;
import com.codexparas.ai_gateway.openai.service.OpenAiInferenceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v1/inference", "/v1"})
public class InferenceController {

  private final OpenAiInferenceService inferenceService;

  public InferenceController(OpenAiInferenceService inferenceService) {
    this.inferenceService = inferenceService;
  }

  @PostMapping("/responses")
  public Mono<ResponseEntity<ResponsesResponse>> createResponse(
      @Valid @RequestBody ResponsesRequest request) {
    return inferenceService.createResponse(request).map(ResponseEntity::ok);
  }

  @PostMapping(value = "/responses/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> streamResponse(
      @Valid @RequestBody ResponsesRequest request) {
    return inferenceService.streamResponse(request);
  }

  @PostMapping("/chat/completions")
  public Mono<ChatCompletionResponse> createChatCompletion(
      @Valid @RequestBody ChatCompletionsRequest request) {
    return inferenceService.createChatCompletion(request);
  }

  @PostMapping("/embeddings")
  public Mono<EmbeddingResponse> createEmbedding(@Valid @RequestBody EmbeddingsRequest request) {
    return inferenceService.createEmbedding(request);
  }

  @GetMapping("/models")
  public Mono<ModelListResponse> listModels() {
    return inferenceService.listModels();
  }

  @GetMapping("/models/{model}")
  public Mono<ModelResponse> retrieveModel(@PathVariable String model) {
    return inferenceService.retrieveModel(model);
  }

  @PostMapping(value = "/audio/speech", produces = "audio/mpeg")
  public Mono<byte[]> createSpeech(@Valid @RequestBody SpeechRequest request) {
    return inferenceService.createSpeech(request);
  }

  @PostMapping(value = "/audio/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<TranscriptionResponse> createTranscription(
      @RequestPart("file") MultipartFile file,
      @RequestParam String model,
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String prompt,
      @RequestParam(name = "response_format", required = false) String responseFormat,
      @RequestParam(required = false) Double temperature,
      @RequestParam(name = "timestamp_granularities[]", required = false)
          String timestampGranularities) {
    TranscriptionRequest request =
        new TranscriptionRequest(
            model, language, prompt, responseFormat, temperature, timestampGranularities);
    try {
      return inferenceService.createTranscription(
          request,
          file.getBytes(),
          file.getOriginalFilename() == null ? "audio" : file.getOriginalFilename(),
          file.getContentType() == null ? "application/octet-stream" : file.getContentType());
    } catch (java.io.IOException exception) {
      return Mono.error(exception);
    }
  }
}
