package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.config.GatewayStreamingProperties;
import com.codexparas.ai_gateway.exception.GuardrailViolationException;
import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class GatewayResponseFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;
  private final GatewayStreamingProperties streamingProperties;
  private final Map<String, ResponseTransformer> responseTransformers;
  private final Map<String, StreamingResponseTransformer> streamingTransformers;

  public GatewayResponseFilter(
      ObjectMapper objectMapper,
      GatewayStreamingProperties streamingProperties,
      List<ResponseTransformer> responseTransformers,
      List<StreamingResponseTransformer> streamingTransformers) {
    this.objectMapper = objectMapper;
    this.streamingProperties = streamingProperties;
    this.responseTransformers = index(responseTransformers);
    this.streamingTransformers = index(streamingTransformers);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    GatewayRoute route = (GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE);
    if (route == null) {
      chain.doFilter(request, response);
      return;
    }

    @SuppressWarnings("unchecked")
    Flux<ChatResponse> stream =
        (Flux<ChatResponse>) request.getAttribute(GatewayRequestAttributes.CHAT_RESPONSE_STREAM);
    if (stream != null) {
      writeStream(request, response, route, stream);
      return;
    }

    ChatResponse chatResponse =
        (ChatResponse) request.getAttribute(GatewayRequestAttributes.CHAT_RESPONSE);
    if (chatResponse == null) {
      chain.doFilter(request, response);
      return;
    }

    ResponseTransformer transformer =
        responseTransformers.get(route.provider().toUpperCase(Locale.ROOT));
    if (transformer == null || !transformer.supports(route)) {
      writeError(
          response, "response_transformer_not_found", "No response transformer is configured");
      return;
    }
    ProviderResponse providerResponse = transformer.transform(route, chatResponse);
    storeResponseMetadata(request, chatResponse);
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), providerResponse.body());
  }

  private void writeStream(
      HttpServletRequest request,
      HttpServletResponse response,
      GatewayRoute route,
      Flux<ChatResponse> stream)
      throws IOException {
    StreamingResponseTransformer transformer =
        streamingTransformers.get(route.provider().toUpperCase(Locale.ROOT));
    if (transformer == null || !transformer.supports(route)) {
      writeError(
          response, "streaming_transformer_not_found", "No streaming transformer is configured");
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("X-Accel-Buffering", "no");

    AsyncContext asyncContext =
        request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();
    asyncContext.setTimeout(streamingProperties.timeout().toMillis());
    PrintWriter writer = response.getWriter();
    AtomicReference<Disposable> subscriptionReference = new AtomicReference<>();
    AtomicBoolean terminated = new AtomicBoolean();
    asyncContext.addListener(
        new StreamLifecycleListener(request, subscriptionReference, terminated));
    Disposable subscription =
        stream.subscribe(
            chunk -> {
              if (request.getAttribute(GatewayRequestAttributes.STREAM_FIRST_TOKEN_AT) == null) {
                request.setAttribute(GatewayRequestAttributes.STREAM_FIRST_TOKEN_AT, Instant.now());
              }
              storeResponseMetadata(request, chunk);
              writeChunk(writer, transformer.transformChunk(route, chunk));
            },
            error -> completeWithError(request, route, asyncContext, writer, error),
            () -> completeStream(route, asyncContext, writer));
    subscriptionReference.set(subscription);
    if (terminated.get()) subscription.dispose();
    request.setAttribute(
        GatewayRequestAttributes.CHAT_RESPONSE_STREAM + ".subscription", subscription);
  }

  private void writeChunk(PrintWriter writer, ProviderResponse providerResponse) {
    synchronized (writer) {
      if (providerResponse.eventName() != null && !providerResponse.eventName().isBlank()) {
        writer.write("event: " + providerResponse.eventName() + "\n");
      }
      writer.write("data: ");
      objectMapper.writeValue(writer, providerResponse.body());
      writer.write("\n\n");
      writer.flush();
    }
  }

  private void completeStream(GatewayRoute route, AsyncContext asyncContext, PrintWriter writer) {
    synchronized (writer) {
      if (route.provider().equalsIgnoreCase("OPENAI")
          || route.provider().equalsIgnoreCase("AZURE")) {
        writer.write("data: [DONE]\n\n");
        writer.flush();
      }
    }
    asyncContext.complete();
  }

  private void completeWithError(
      HttpServletRequest request,
      GatewayRoute route,
      AsyncContext asyncContext,
      PrintWriter writer,
      Throwable error) {
    Throwable normalized = normalizeStreamError(request, route, error);
    request.setAttribute(GatewayRequestAttributes.ERROR, normalized);
    request.setAttribute(
        GatewayRequestAttributes.STREAM_TERMINATION, streamTermination(normalized));
    synchronized (writer) {
      if (!writer.checkError()) {
        String code =
            normalized instanceof GuardrailViolationException
                ? "response_guardrail_blocked"
                : normalized instanceof TimeoutException
                    ? "stream_timeout"
                    : "stream_execution_failed";
        writer.write("event: error\n");
        writer.write("data: {\"error\":\"" + code + "\"}\n\n");
        writer.flush();
      }
    }
    asyncContext.complete();
  }

  private void writeError(HttpServletResponse response, String error, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), Map.of("error", error, "message", message));
  }

  private static <T> Map<String, T> index(List<T> transformers) {
    return transformers.stream()
        .collect(
            Collectors.toUnmodifiableMap(
                transformer -> provider(transformer).toUpperCase(Locale.ROOT),
                Function.identity()));
  }

  private static String provider(Object transformer) {
    if (transformer instanceof ResponseTransformer responseTransformer) {
      return responseTransformer.provider();
    }
    return ((StreamingResponseTransformer) transformer).provider();
  }

  private static void storeResponseMetadata(HttpServletRequest request, ChatResponse response) {
    if (response.getMetadata() != null && response.getMetadata().getId() != null) {
      request.setAttribute(
          GatewayRequestAttributes.PROVIDER_REQUEST_ID, response.getMetadata().getId());
    }
    if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
      org.springframework.ai.chat.metadata.Usage usage = response.getMetadata().getUsage();
      request.setAttribute(
          GatewayRequestAttributes.USAGE,
          new com.codexparas.ai_gateway.logging.GatewayAuditEvent.Usage(
              nullableLong(usage.getPromptTokens()),
              nullableLong(usage.getCompletionTokens()),
              nullableLong(usage.getTotalTokens()),
              null,
              null));
    }
  }

  private static Long nullableLong(Integer value) {
    return value == null ? null : value.longValue();
  }

  private static Throwable normalizeStreamError(
      HttpServletRequest request, GatewayRoute route, Throwable error) {
    if (error instanceof GuardrailViolationException
        || error instanceof TimeoutException
        || error instanceof CancellationException) {
      return error;
    }
    com.codexparas.ai_gateway.model.PreparedChatModel prepared =
        (com.codexparas.ai_gateway.model.PreparedChatModel)
            request.getAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL);
    return prepared == null
        ? error
        : new ProviderExecutionException(prepared.provider(), prepared.model(), error);
  }

  private static String streamTermination(Throwable error) {
    if (error instanceof TimeoutException) return "TIMEOUT";
    if (error instanceof CancellationException) return "CANCELLED";
    if (error instanceof GuardrailViolationException) return "GUARDRAIL_BLOCKED";
    return "PROVIDER_ERROR";
  }

  private static final class StreamLifecycleListener implements AsyncListener {
    private final HttpServletRequest request;
    private final AtomicReference<Disposable> subscription;
    private final AtomicBoolean terminated;

    private StreamLifecycleListener(
        HttpServletRequest request,
        AtomicReference<Disposable> subscription,
        AtomicBoolean terminated) {
      this.request = request;
      this.subscription = subscription;
      this.terminated = terminated;
    }

    @Override
    public void onComplete(AsyncEvent event) {
      terminated.set(true);
      if (request.getAttribute(GatewayRequestAttributes.STREAM_TERMINATION) == null) {
        request.setAttribute(GatewayRequestAttributes.STREAM_TERMINATION, "COMPLETED");
      }
      dispose();
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      terminated.set(true);
      request.setAttribute(
          GatewayRequestAttributes.ERROR, new TimeoutException("stream timed out"));
      request.setAttribute(GatewayRequestAttributes.STREAM_TERMINATION, "TIMEOUT");
      dispose();
    }

    @Override
    public void onError(AsyncEvent event) {
      terminated.set(true);
      request.setAttribute(GatewayRequestAttributes.ERROR, event.getThrowable());
      request.setAttribute(
          GatewayRequestAttributes.STREAM_TERMINATION,
          event.getThrowable() instanceof java.io.IOException ? "DISCONNECTED" : "STREAM_ERROR");
      dispose();
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
      // The current async request owns the subscription.
    }

    private void dispose() {
      Disposable current = subscription.get();
      if (current != null) current.dispose();
    }
  }
}
