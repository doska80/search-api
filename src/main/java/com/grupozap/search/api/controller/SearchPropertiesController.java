package com.grupozap.search.api.controller;

import static org.springframework.http.HttpStatus.OK;

import com.grupozap.search.api.service.SearchApiEnvService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/properties")
@Api("v2")
public class SearchPropertiesController {

  @Autowired private SearchApiEnvService service;

  @GetMapping("/local")
  @ApiOperation(value = "Get local properties", notes = "Returns all local properties")
  @ApiResponse(code = 200, message = "Returns successfully all local properties")
  public Object getLocalProperties() {
    return new ResponseEntity<>(service.getLocalProperties(), OK);
  }

  @GetMapping("/remote")
  @ApiOperation(value = "Get remote properties", notes = "Returns all remote properties")
  @ApiResponse(code = 200, message = "Returns successfully all remote properties")
  public Object getRemoteProperties() {
    return new ResponseEntity<>(service.getRemoteProperties(), OK);
  }
}
