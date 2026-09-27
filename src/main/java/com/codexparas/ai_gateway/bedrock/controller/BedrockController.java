package com.codexparas.ai_gateway.bedrock.controller;

import com.codexparas.ai_gateway.bedrock.dto.BedrockConverseRequest;
import com.codexparas.ai_gateway.bedrock.dto.BedrockConverseResponse;
import com.codexparas.ai_gateway.bedrock.dto.BedrockModelList;
import com.codexparas.ai_gateway.bedrock.service.BedrockInferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/bedrock")
public class BedrockController {

  private final BedrockInferenceService inferenceService;

  public BedrockController(BedrockInferenceService inferenceService) {
    this.inferenceService = inferenceService;
  }

  @PostMapping("/models/{modelId}/converse")
  public Mono<BedrockConverseResponse> converse(
      @PathVariable @NotBlank String modelId, @Valid @RequestBody BedrockConverseRequest request) {
    return inferenceService.converse(modelId, request);
  }

  @GetMapping("/models")
  public Mono<BedrockModelList> listModels() {
    return inferenceService.listModels();
  }
}
