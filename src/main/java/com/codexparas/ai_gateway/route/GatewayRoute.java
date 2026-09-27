package com.codexparas.ai_gateway.route;

/** Normalized protocol route selected for a gateway request. */
public record GatewayRoute(String provider, String operation, boolean streaming) {}
