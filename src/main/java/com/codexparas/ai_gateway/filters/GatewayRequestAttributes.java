package com.codexparas.ai_gateway.filters;

public final class GatewayRequestAttributes {
  public static final String MODEL_NAME = GatewayRequestAttributes.class.getName() + ".model";
  public static final String ROUTE = GatewayRequestAttributes.class.getName() + ".route";
  public static final String MODEL_CONFIGURATION =
      GatewayRequestAttributes.class.getName() + ".configuration";
  public static final String CHAT_MODEL = GatewayRequestAttributes.class.getName() + ".chatModel";
  public static final String TRANSFORMED_REQUEST =
      GatewayRequestAttributes.class.getName() + ".transformedRequest";
  public static final String CHAT_RESPONSE =
      GatewayRequestAttributes.class.getName() + ".chatResponse";
  public static final String CHAT_RESPONSE_STREAM =
      GatewayRequestAttributes.class.getName() + ".chatResponseStream";
  public static final String PREPARED_CHAT_MODEL =
      GatewayRequestAttributes.class.getName() + ".preparedChatModel";
  public static final String REQUEST_BODY =
      GatewayRequestAttributes.class.getName() + ".requestBody";
  public static final String ERROR = GatewayRequestAttributes.class.getName() + ".error";
  public static final String GUARDRAIL_DECISION =
      GatewayRequestAttributes.class.getName() + ".guardrailDecision";
  public static final String RESPONSE_GUARDRAIL_DECISION =
      GatewayRequestAttributes.class.getName() + ".responseGuardrailDecision";
  public static final String UPSTREAM_CREDENTIAL =
      GatewayRequestAttributes.class.getName() + ".upstreamCredential";

  private GatewayRequestAttributes() {}
}
