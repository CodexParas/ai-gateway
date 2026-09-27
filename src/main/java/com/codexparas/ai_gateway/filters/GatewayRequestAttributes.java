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
  public static final String PROVIDER_REQUEST_ID =
      GatewayRequestAttributes.class.getName() + ".providerRequestId";
  public static final String PROVIDER_STATUS =
      GatewayRequestAttributes.class.getName() + ".providerStatus";
  public static final String PROVIDER_ERROR_CODE =
      GatewayRequestAttributes.class.getName() + ".providerErrorCode";
  public static final String RETRY_COUNT = GatewayRequestAttributes.class.getName() + ".retryCount";
  public static final String FALLBACK_USED =
      GatewayRequestAttributes.class.getName() + ".fallbackUsed";
  public static final String USAGE = GatewayRequestAttributes.class.getName() + ".usage";
  public static final String STREAM_FIRST_TOKEN_AT =
      GatewayRequestAttributes.class.getName() + ".streamFirstTokenAt";
  public static final String STREAM_TERMINATION =
      GatewayRequestAttributes.class.getName() + ".streamTermination";
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
