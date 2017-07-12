package com.vivareal.search.api.controller;

import com.vivareal.search.api.service.ClusterSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping({"/v2", "/v2"})
public class ClusterSettingsController {

    @Autowired
    private ClusterSettingsService clusterSettingsService;

    @RequestMapping(value = {"/cluster/settings"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Map<String, Object>> getSettings() {
        return clusterSettingsService.settings();
    }
}
