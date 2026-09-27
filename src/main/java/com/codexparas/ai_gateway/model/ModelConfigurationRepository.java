package com.codexparas.ai_gateway.model;

import com.codexparas.ai_gateway.config.ModelConfigurationProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class ModelConfigurationRepository {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ModelConfigurationProperties properties;

  public ModelConfigurationRepository(
      ReactiveStringRedisTemplate redisTemplate, ModelConfigurationProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  public Mono<String> findByModel(String model) {
    return redisTemplate.opsForHash().get(properties.redisHash(), model).cast(String.class);
  }
}
