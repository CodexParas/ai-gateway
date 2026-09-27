package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModelConfigurationProperties.class)
public class ModelConfigurationClientConfiguration {}
