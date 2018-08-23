package com.grupozap.search.api.controller.error;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ExceptionController implements ErrorController {

  private static final String ERROR_PATH = "/error";

  @Autowired private ExceptionHandler exceptionHandler;

  @ResponseBody
  @RequestMapping(value = ERROR_PATH)
  public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
    return exceptionHandler.error(request);
  }

  @Override
  public String getErrorPath() {
    return ERROR_PATH;
  }
}
