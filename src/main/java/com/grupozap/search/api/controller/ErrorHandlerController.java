package com.grupozap.search.api.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.valueOf;

import com.grupozap.search.api.exception.QueryPhaseExecutionException;
import com.grupozap.search.api.exception.QueryTimeoutException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.catalina.connector.ClientAbortException;
import org.elasticsearch.ElasticsearchStatusException;
import org.jparsec.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice(basePackages = "com.grupozap.search.api.controller")
public class ErrorHandlerController extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlerController.class);

  @ExceptionHandler(
      value = {
        IllegalArgumentException.class,
        InvalidPropertyException.class,
        ParserException.class
      })
  public ResponseEntity<Object> handleInvalidRequest(Exception ex, WebRequest request) {
    return handleExceptionInternal(ex, null, new HttpHeaders(), BAD_REQUEST, request);
  }

  @ExceptionHandler(value = {QueryTimeoutException.class, TimeoutException.class})
  public ResponseEntity<Object> handleTimeout(Exception ex, WebRequest request) {
    return handleExceptionInternal(ex, null, new HttpHeaders(), GATEWAY_TIMEOUT, request);
  }

  @ExceptionHandler(value = {QueryPhaseExecutionException.class})
  public ResponseEntity<Object> handleQueryPhaseExecution(Exception ex, WebRequest request) {
    final HttpStatus status;
    if (ex.getCause() instanceof ElasticsearchStatusException) {
      status = valueOf(((ElasticsearchStatusException) ex.getCause()).status().getStatus());
    } else {
      status = INTERNAL_SERVER_ERROR;
    }
    return handleExceptionInternal(ex, null, new HttpHeaders(), status, request);
  }

  @ExceptionHandler(value = {CallNotPermittedException.class})
  public ResponseEntity<Object> handleOpenCircuit(Exception ex, WebRequest request) {
    return handleExceptionInternal(ex, null, new HttpHeaders(), SERVICE_UNAVAILABLE, request);
  }

  @ExceptionHandler(value = {ClientAbortException.class})
  public void handleBrokenPipe(Exception ex, WebRequest request) {
    LOG.debug("broken pipe {}", request.getContextPath(), ex);
  }

  @ExceptionHandler(value = {Exception.class})
  public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
    return handleExceptionInternal(ex, null, new HttpHeaders(), INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object originalBody,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    final var body = null == originalBody ? defaultBodyFor(ex, status, request) : originalBody;
    if (status.is4xxClientError()) {
      LOG.debug("invalid client request", ex);
    }
    if (status.is5xxServerError()) {
      LOG.error("internal api error", ex);
    }
    return super.handleExceptionInternal(ex, body, headers, status, request);
  }

  private Map<String, Object> defaultBodyFor(Exception ex, HttpStatus status, WebRequest request) {
    final var body = new LinkedHashMap<String, Object>();
    body.put("timestamp", new Date());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", ex.getMessage());
    body.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());
    body.put("request", request.getParameterMap());
    return body;
  }
}
