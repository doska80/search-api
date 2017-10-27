package com.vivareal.search.api.controller.error;

import com.vivareal.search.api.exception.QueryPhaseExecutionException;
import com.vivareal.search.api.exception.QueryTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;

@RequestScope
@Component
public class ExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

    @Autowired
    private ErrorAttributes errorAttributes;

    public ResponseEntity<Map<String, Object>> error(Throwable e) {
        return error(e, ((ServletRequestAttributes) currentRequestAttributes()).getRequest());
    }

    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        return error(errorAttributes.getError(new DispatcherServletWebRequest(request)), request);
    }

    public ResponseEntity<Map<String, Object>> error(Throwable e, HttpServletRequest request) {
        Map<String, Object> errorBody = errorAttributes.getErrorAttributes(new DispatcherServletWebRequest(request), getTraceParameter(request));
        HttpStatus status = getStatus(e, request, errorBody);

        errorBody.put("request", request.getParameterMap());

        return new ResponseEntity<>(errorBody, status);
    }

    private boolean getTraceParameter(HttpServletRequest request) {
        String parameter = request.getParameter("trace");
        return parameter != null && !"FALSE".equalsIgnoreCase(parameter);
    }

    private HttpStatus setResponseStatus(Map<String, Object> errorBody, HttpStatus httpStatus) {
        errorBody.put("status", httpStatus.value());
        errorBody.put("error", httpStatus.getReasonPhrase());
        return httpStatus;
    }

    private HttpStatus getStatus(Throwable e, HttpServletRequest request, Map<String, Object> errorBody) {
        if(e != null) {
            String rootCauseMessage = getRootCauseMessage(e);
            errorBody.put("message", rootCauseMessage);

            if(e instanceof IllegalArgumentException || getRootCause(e) instanceof IllegalArgumentException)
                return setResponseStatus(errorBody, BAD_REQUEST);

            if (e instanceof QueryPhaseExecutionException) {
                LOG.error("Path: [{}] - Request Parameters: [{}] - RootCauseMessage: [{}] - Additional Message: [{}]",
                errorBody.getOrDefault("path", "None"),
                getParametersFromRequest(request),
                rootCauseMessage,
                "Query: " + ((QueryPhaseExecutionException) e).getQuery()
                );

                if (e instanceof QueryTimeoutException || getRootCause(e) instanceof TimeoutException)
                    return setResponseStatus(errorBody, GATEWAY_TIMEOUT);
            }
        }

        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            try {
                return setResponseStatus(errorBody, HttpStatus.valueOf(statusCode));
            } catch (Exception ex) {
                LOG.error("Invalid http status code", ex);
            }
        }

        return setResponseStatus(errorBody, INTERNAL_SERVER_ERROR);
    }

    private String getParametersFromRequest(final HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream().map(e -> format("%s=%s", e.getKey(), join(e.getValue()))).collect(joining("&"));
    }
}
