package com.codexparas.ai_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GatewayStreamingProperties.class, GatewayExecutionProperties.class})
public class GatewayStreamingConfiguration {}
