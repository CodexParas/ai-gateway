package com.codexparas.ai_gateway.openai.service;

import com.codexparas.ai_gateway.openai.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Service
public class OpenAiInferenceService {

  private final WebClient openAiWebClient;

  public OpenAiInferenceService(@Qualifier("openAiWebClient") WebClient openAiWebClient) {
    this.openAiWebClient = openAiWebClient;
  }

  private static void addPart(MultipartBodyBuilder body, String name, String value) {
    if (value != null) {
      body.part(name, value);
    }
  }

  public Mono<ResponsesResponse> createResponse(ResponsesRequest request) {
    return openAiWebClient
        .post()
        .uri("/v1/responses")
        .bodyValue(request)
        .exchangeToMono(
            response -> {
              String requestId =
                  response.headers().header("x-request-id").stream().findFirst().orElse(null);
              if (response.statusCode().isError()) {
                return response
                    .bodyToMono(JsonNode.class)
                    .flatMap(
                        body ->
                            Mono.<ResponsesResponse>error(
                                new OpenAiInferenceException(
                                    response.statusCode(), requestId, body)))
                    .switchIfEmpty(
                        Mono.error(
                            new OpenAiInferenceException(response.statusCode(), requestId, null)));
              }
              return response.bodyToMono(ResponsesResponse.class);
            });
  }

  public Flux<ServerSentEvent<String>> streamResponse(ResponsesRequest request) {
    return openAiWebClient
        .post()
        .uri("/v1/responses")
        .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<>() {});
  }

  public Mono<ChatCompletionResponse> createChatCompletion(ChatCompletionsRequest request) {
    return postJson("/v1/chat/completions", request, ChatCompletionResponse.class);
  }

  public Mono<EmbeddingResponse> createEmbedding(EmbeddingsRequest request) {
    return postJson("/v1/embeddings", request, EmbeddingResponse.class);
  }

  public Mono<ModelListResponse> listModels() {
    return openAiWebClient.get().uri("/v1/models").retrieve().bodyToMono(ModelListResponse.class);
  }

  public Mono<ModelResponse> retrieveModel(String model) {
    return openAiWebClient
        .get()
        .uri("/v1/models/{model}", model)
        .retrieve()
        .bodyToMono(ModelResponse.class);
  }

  public Mono<byte[]> createSpeech(SpeechRequest request) {
    return openAiWebClient
        .post()
        .uri("/v1/audio/speech")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(byte[].class);
  }

  public Mono<TranscriptionResponse> createTranscription(
      TranscriptionRequest request, byte[] audio, String filename, String contentType) {
    MultipartBodyBuilder body = new MultipartBodyBuilder();
    body.part("file", new ByteArrayResource(audio) {})
        .filename(filename)
        .contentType(MediaType.parseMediaType(contentType));
    body.part("model", request.model());
    addPart(body, "language", request.language());
    addPart(body, "prompt", request.prompt());
    addPart(body, "response_format", request.responseFormat());
    if (request.temperature() != null) {
      body.part("temperature", request.temperature());
    }
    addPart(body, "timestamp_granularities[]", request.timestampGranularities());
    return openAiWebClient
        .post()
        .uri("/v1/audio/transcriptions")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(body.build())
        .retrieve()
        .bodyToMono(TranscriptionResponse.class);
  }

  private <T> Mono<T> postJson(String path, Object request, Class<T> responseType) {
    return openAiWebClient.post().uri(path).bodyValue(request).retrieve().bodyToMono(responseType);
  }

  public static class OpenAiInferenceException extends RuntimeException {
    private final HttpStatusCode status;
    private final String requestId;
    private final JsonNode body;

    public OpenAiInferenceException(HttpStatusCode status, String requestId, JsonNode body) {
      super("OpenAI inference request failed with status " + status.value());
      this.status = status;
      this.requestId = requestId;
      this.body = body;
    }

    public HttpStatusCode status() {
      return status;
    }

    public String requestId() {
      return requestId;
    }

    public JsonNode body() {
      return body;
    }
  }
}
