package com.codexparas.ai_gateway.security;

import java.util.List;

public record UpstreamCredential(
    String credentialId,
    String orgId,
    String appId,
    boolean enabled,
    String expiresAt,
    List<String> scopes,
    List<String> allowedModels) {}
