package com.codexparas.ai_gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class UpstreamCredentialService {
  private final UpstreamCredentialRepository repository;
  private final ObjectMapper objectMapper;

  public UpstreamCredentialService(
      UpstreamCredentialRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public static String sha256(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static String text(JsonNode json, String name) {
    return json.path(name).isString() ? json.path(name).asString() : null;
  }

  private static String nullableText(JsonNode json) {
    return text(json, "expiresAt");
  }

  private static List<String> strings(JsonNode node) {
    if (!node.isArray()) return List.of();
    return java.util.stream.StreamSupport.stream(node.spliterator(), false)
        .filter(JsonNode::isString)
        .map(JsonNode::asString)
        .toList();
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public Mono<UpstreamCredential> resolve(String token) {
    String tokenHash = sha256(token);
    return repository
        .findByTokenHash(tokenHash)
        .switchIfEmpty(Mono.error(new CredentialRejectedException("credential_not_found")))
        .flatMap(this::parse)
        .flatMap(this::validate)
        .onErrorMap(
            exception -> !(exception instanceof CredentialRejectedException),
            CredentialStoreException::new);
  }

  private Mono<UpstreamCredential> parse(String raw) {
    try {
      JsonNode json = objectMapper.readTree(raw);
      return Mono.just(
          new UpstreamCredential(
              text(json, "credentialId"),
              text(json, "orgId"),
              text(json, "appId"),
              json.path("enabled").asBoolean(false),
              nullableText(json),
              strings(json.path("scopes")),
              strings(json.path("allowedModels"))));
    } catch (JacksonException exception) {
      return Mono.error(new CredentialRejectedException("credential_invalid", exception));
    }
  }

  private Mono<UpstreamCredential> validate(UpstreamCredential credential) {
    if (blank(credential.credentialId())
        || blank(credential.orgId())
        || blank(credential.appId())) {
      return Mono.error(new CredentialRejectedException("credential_identity_incomplete"));
    }
    if (!credential.enabled()) {
      return Mono.error(new CredentialRejectedException("credential_disabled"));
    }
    if (credential.expiresAt() != null
        && Instant.parse(credential.expiresAt()).isBefore(Instant.now())) {
      return Mono.error(new CredentialRejectedException("credential_expired"));
    }
    return Mono.just(credential);
  }

  public static class CredentialRejectedException extends RuntimeException {
    public CredentialRejectedException(String message) {
      super(message);
    }

    public CredentialRejectedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class CredentialStoreException extends RuntimeException {
    public CredentialStoreException(Throwable cause) {
      super("credential_store_unavailable", cause);
    }
  }
}
