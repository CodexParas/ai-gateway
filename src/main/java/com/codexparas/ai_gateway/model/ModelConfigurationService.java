package com.codexparas.ai_gateway.model;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ModelConfigurationService {

  private final ModelConfigurationRepository repository;
  private final ObjectMapper objectMapper;
  private final List<ModelConfigurationValidator> validators;

  public ModelConfigurationService(
      ModelConfigurationRepository repository,
      ObjectMapper objectMapper,
      List<ModelConfigurationValidator> validators) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.validators = validators;
  }

  public Mono<JsonNode> findAndValidate(String model) {
    return repository
        .findByModel(model)
        .switchIfEmpty(Mono.error(new ModelConfigurationNotFoundException(model)))
        .flatMap(raw -> parseAndWrap(model, raw))
        .flatMap(this::validate)
        .onErrorMap(
            exception -> !(exception instanceof ModelConfigurationException),
            exception -> new ModelConfigurationLookupException(model, exception));
  }

  private Mono<JsonNode> parse(String model, String raw) {
    try {
      return Mono.just(objectMapper.readTree(raw));
    } catch (JacksonException exception) {
      return Mono.error(
          new ModelConfigurationInvalidException(model, List.of("invalid JSON"), exception));
    }
  }

  private Mono<JsonNode> validate(ValidatedConfiguration configuration) {
    List<String> errors =
        validators.stream()
            .flatMap(
                validator ->
                    validator.validate(configuration.model(), configuration.json()).stream())
            .collect(Collectors.toList());
    return errors.isEmpty()
        ? Mono.just(configuration.json())
        : Mono.error(new ModelConfigurationInvalidException(configuration.model(), errors));
  }

  private Mono<ValidatedConfiguration> parseAndWrap(String model, String raw) {
    return parse(model, raw).map(json -> new ValidatedConfiguration(model, json));
  }

  private record ValidatedConfiguration(String model, JsonNode json) {}

  public static class ModelConfigurationException extends RuntimeException {
    private final String model;

    protected ModelConfigurationException(String message, String model, Throwable cause) {
      super(message, cause);
      this.model = model;
    }

    protected ModelConfigurationException(String message, String model) {
      super(message);
      this.model = model;
    }

    public String model() {
      return model;
    }
  }

  public static final class ModelConfigurationNotFoundException
      extends ModelConfigurationException {
    public ModelConfigurationNotFoundException(String model) {
      super("No model configuration found", model);
    }
  }

  public static final class ModelConfigurationLookupException extends ModelConfigurationException {
    public ModelConfigurationLookupException(String model, Throwable cause) {
      super("Model configuration lookup failed", model, cause);
    }
  }

  public static final class ModelConfigurationInvalidException extends ModelConfigurationException {
    private final List<String> errors;

    public ModelConfigurationInvalidException(String model, List<String> errors) {
      super("Model configuration validation failed", model);
      this.errors = List.copyOf(errors);
    }

    public ModelConfigurationInvalidException(String model, List<String> errors, Throwable cause) {
      super("Model configuration validation failed", model, cause);
      this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
      return errors;
    }
  }
}
