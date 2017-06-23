package com.vivareal.search.api.controller;

import com.vivareal.search.api.model.Healthcheck;

import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthcheck")
public class HealthcheckController {

    @Autowired
    private TransportClient client;

    @RequestMapping("/status")
    public Healthcheck status() {
        return new Healthcheck(client);
    }
}
