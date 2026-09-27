package com.codexparas.ai_gateway.bedrock.service;

import com.codexparas.ai_gateway.bedrock.dto.BedrockConverseRequest;
import com.codexparas.ai_gateway.bedrock.dto.BedrockConverseResponse;
import com.codexparas.ai_gateway.bedrock.dto.BedrockMessage;
import com.codexparas.ai_gateway.bedrock.dto.BedrockModel;
import com.codexparas.ai_gateway.bedrock.dto.BedrockModelList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsRequest;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

@Service
public class BedrockInferenceService {

  private final BedrockRuntimeClient runtimeClient;
  private final BedrockClient bedrockClient;

  public BedrockInferenceService(BedrockRuntimeClient runtimeClient, BedrockClient bedrockClient) {
    this.runtimeClient = runtimeClient;
    this.bedrockClient = bedrockClient;
  }

  public Mono<BedrockConverseResponse> converse(String modelId, BedrockConverseRequest request) {
    return Mono.fromCallable(
        () -> mapResponse(modelId, runtimeClient.converse(toSdkRequest(modelId, request))));
  }

  public Mono<BedrockModelList> listModels() {
    return Mono.fromCallable(
        () ->
            new BedrockModelList(
                bedrockClient
                    .listFoundationModels(ListFoundationModelsRequest.builder().build())
                    .modelSummaries()
                    .stream()
                    .map(BedrockInferenceService::mapModel)
                    .toList()));
  }

  private static ConverseRequest toSdkRequest(String modelId, BedrockConverseRequest request) {
    ConverseRequest.Builder builder =
        ConverseRequest.builder()
            .modelId(modelId)
            .messages(
                request.messages().stream().map(BedrockInferenceService::mapMessage).toList());
    if (request.system() != null) {
      builder.system(SystemContentBlock.fromText(request.system()));
    }
    if (request.inferenceConfig() != null) {
      var config = request.inferenceConfig();
      builder.inferenceConfig(
          InferenceConfiguration.builder()
              .maxTokens(config.maxTokens())
              .temperature(config.temperature() == null ? null : config.temperature().floatValue())
              .topP(config.topP() == null ? null : config.topP().floatValue())
              .stopSequences(config.stopSequences())
              .build());
    }
    if (request.requestMetadata() != null) {
      builder.requestMetadata(request.requestMetadata());
    }
    return builder.build();
  }

  private static Message mapMessage(BedrockMessage message) {
    return Message.builder()
        .role(ConversationRole.fromValue(message.role()))
        .content(
            message.content().stream()
                .map(content -> ContentBlock.fromText(content.text()))
                .toList())
        .build();
  }

  private static BedrockConverseResponse mapResponse(String modelId, ConverseResponse response) {
    String text =
        response.output().message().content().stream()
            .map(ContentBlock::text)
            .filter(Objects::nonNull)
            .collect(Collectors.joining());
    var usage = response.usage();
    return new BedrockConverseResponse(
        modelId,
        response.output().message().roleAsString(),
        text,
        response.stopReasonAsString(),
        usage == null ? null : usage.inputTokens(),
        usage == null ? null : usage.outputTokens(),
        usage == null ? null : usage.totalTokens());
  }

  private static BedrockModel mapModel(FoundationModelSummary model) {
    return new BedrockModel(
        model.modelId(),
        model.modelName(),
        model.providerName(),
        model.inputModalitiesAsStrings(),
        model.outputModalitiesAsStrings(),
        model.responseStreamingSupported());
  }
}
