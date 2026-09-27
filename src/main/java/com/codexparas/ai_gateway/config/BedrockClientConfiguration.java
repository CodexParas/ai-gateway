package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
@EnableConfigurationProperties(BedrockProperties.class)
public class BedrockClientConfiguration {

  @Bean
  BedrockRuntimeClient bedrockRuntimeClient(BedrockProperties properties) {
    return BedrockRuntimeClient.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

  @Bean
  BedrockClient bedrockClient(BedrockProperties properties) {
    return BedrockClient.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}
