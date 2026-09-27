package com.codexparas.ai_gateway.filters;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.jspecify.annotations.NonNull;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
  private final byte[] body;

  public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body.clone();
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream input = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public int read() {
        return input.read();
      }

      @Override
      public boolean isFinished() {
        return input.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(@NonNull ReadListener readListener) {
        try {
          readListener.onAllDataRead();
        } catch (IOException ignored) {
          // The cached request is already fully available synchronously.
        }
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    Charset charset =
        Charset.forName(getCharacterEncoding() == null ? "UTF-8" : getCharacterEncoding());
    return new BufferedReader(new InputStreamReader(getInputStream(), charset));
  }

  @Override
  public int getContentLength() {
    return body.length;
  }

  @Override
  public long getContentLengthLong() {
    return body.length;
  }
}
