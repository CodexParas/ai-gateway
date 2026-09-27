package com.codexparas.ai_gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gateway.streaming")
public record GatewayStreamingProperties(Duration timeout) {}
