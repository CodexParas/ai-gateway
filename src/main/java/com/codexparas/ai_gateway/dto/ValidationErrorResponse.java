package com.codexparas.ai_gateway.dto;

import java.util.List;

public record ValidationErrorResponse(String error, List<FieldError> fields) {
  public record FieldError(String field, String message) {}
}
