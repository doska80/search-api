package com.vivareal.search.api.controller;

import com.vivareal.search.api.service.SearchApiEnvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/v2/search-api/properties")
public class SearchApiEnvController {

    @Autowired
    private SearchApiEnvService service;

    @RequestMapping(value = {"/local"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getProperties() {
        return new ResponseEntity<>(service.getLocalProperties(), OK);
    }

    @RequestMapping(value = {"/remote"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getRemoteProperties() {
        return new ResponseEntity<>(service.getRemoteProperties(), OK);
    }
}
