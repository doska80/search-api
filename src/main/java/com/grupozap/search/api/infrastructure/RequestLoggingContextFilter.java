package com.grupozap.search.api.infrastructure;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingContextFilter extends OncePerRequestFilter {

  static final String REQUEST_URI = "request_url";
  static final String REQUEST_QUERY_STRING = "request_query_string";
  static final String REQUEST_USER_AGENT = "request_user_agent";
  static final String REQUEST_HEADER_USER_AGENT = "User-Agent";
  private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingContextFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    set(request);
    try {
      LOG.trace("handling request");
      chain.doFilter(request, response);
    } finally {
      clear();
    }
  }

  private void set(HttpServletRequest request) {
    set(REQUEST_URI, request.getRequestURI());
    set(REQUEST_QUERY_STRING, request.getQueryString());
    set(REQUEST_USER_AGENT, request.getHeader(REQUEST_HEADER_USER_AGENT));
  }

  private void set(String key, String value) {
    if (null != value && !value.isEmpty()) {
      put(key, value);
    }
  }

  private void clear() {
    remove(REQUEST_URI);
    remove(REQUEST_QUERY_STRING);
    remove(REQUEST_USER_AGENT);
  }
}
