package com.vivareal.search.api.controller.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Controller
public class ExceptionController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionController.class);

    private static final String ERROR_PATH = "/error";

    @Autowired
    private ErrorAttributes errorAttributes;

    @ResponseBody
    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> errorBody = errorAttributes.getErrorAttributes(new DispatcherServletWebRequest(request), getTraceParameter(request));
        HttpStatus status = getStatus(request, errorBody);

        errorBody.put("request", request.getParameterMap());

        return new ResponseEntity<>(errorBody, status);
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    private boolean getTraceParameter(HttpServletRequest request) {
        String parameter = request.getParameter("trace");
        return parameter != null && !"FALSE".equalsIgnoreCase(parameter);
    }

    private HttpStatus getStatus(HttpServletRequest request, Map<String, Object> errorBody) {
        Throwable e = errorAttributes.getError(new DispatcherServletWebRequest(request));
        if(e != null) {
            errorBody.put("message", getRootCauseMessage(e));

            if(e instanceof IllegalArgumentException || getRootCause(e) instanceof IllegalArgumentException) {
                errorBody.put("status", BAD_REQUEST.value());
                errorBody.put("error", BAD_REQUEST.getReasonPhrase());
                return BAD_REQUEST;
            }
        }

        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (Exception ex) {
                LOG.error("Invalid http status code", ex);
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
