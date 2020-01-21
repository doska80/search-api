package com.grupozap.search.api.controller;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.grupozap.search.api.service.ClusterSettingsService;
import com.grupozap.search.api.service.SearchApiEnvService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2")
@Api("v2")
public class ClusterSettingsController {

  @Autowired private ClusterSettingsService clusterSettingsService;

  @Autowired private SearchApiEnvService service;

  @RequestMapping(
      value = "/cluster/settings",
      method = GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Get all configs", notes = "Returns cluster configurations")
  @ApiResponse(code = 200, message = "Returns successfully all configs")
  public Map<String, Map<String, Object>> getSettings() {
    return clusterSettingsService.settings();
  }

  @RequestMapping(
      value = "/properties/local",
      method = GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Get local properties", notes = "Returns all local properties")
  @ApiResponse(code = 200, message = "Returns successfully all local properties")
  public Object getProperties() {
    return new ResponseEntity<>(service.getLocalProperties(), OK);
  }

  @RequestMapping(
      value = "/properties/remote",
      method = GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Get remote properties", notes = "Returns all remote properties")
  @ApiResponse(code = 200, message = "Returns successfully all remote properties")
  public Object getRemoteProperties() {
    return new ResponseEntity<>(service.getRemoteProperties(), OK);
  }
}
