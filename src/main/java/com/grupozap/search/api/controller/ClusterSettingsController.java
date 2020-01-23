package com.grupozap.search.api.controller;

import com.grupozap.search.api.service.ClusterSettingsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/cluster")
@Api("v2")
public class ClusterSettingsController {

  @Autowired private ClusterSettingsService clusterSettingsService;

  @GetMapping("/settings")
  @ApiOperation(value = "Get all configs", notes = "Returns cluster configurations")
  @ApiResponse(code = 200, message = "Returns successfully all configs")
  public Map<String, Map<String, Object>> getSettings() {
    return clusterSettingsService.settings();
  }
}
