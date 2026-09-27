package com.codexparas.ai_gateway.logging;

import com.codexparas.ai_gateway.config.GatewayLoggingProperties;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class GatewayAuditPublisher {
  private static final Logger log = LoggerFactory.getLogger(GatewayAuditPublisher.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final GatewayLoggingProperties properties;

  public GatewayAuditPublisher(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      GatewayLoggingProperties properties) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public void publish(GatewayAuditEvent event) {
    if (!properties.enabled()) return;
    try {
      String payload = objectMapper.writeValueAsString(event);
      String requestId = event.request().requestId();
      CompletableFuture<?> result = kafkaTemplate.send(properties.topic(), requestId, payload);
      result.whenComplete(
          (ignored, error) -> {
            if (error != null) {
              log.warn("gateway_audit_publish_failed requestId={}", requestId, error);
            }
          });
    } catch (JacksonException exception) {
      log.warn(
          "gateway_audit_serialization_failed requestId={}",
          event.request().requestId(),
          exception);
    } catch (RuntimeException exception) {
      log.warn("gateway_audit_publish_failed requestId={}", event.request().requestId(), exception);
    }
  }
}
