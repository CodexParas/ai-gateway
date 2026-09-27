package com.codexparas.ai_gateway.gemini.dto;

import java.util.List;

public record GeminiModelList(List<GeminiModel> models, String nextPageToken) {}
