package com.codexparas.ai_gateway.security;

import com.codexparas.ai_gateway.config.GatewaySecurityProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UpstreamCredentialRepository {
  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewaySecurityProperties properties;

  public UpstreamCredentialRepository(
      ReactiveStringRedisTemplate redisTemplate, GatewaySecurityProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  public Mono<String> findByTokenHash(String tokenHash) {
    return redisTemplate
        .opsForHash()
        .get(properties.credentialHash(), tokenHash)
        .cast(String.class);
  }
}
