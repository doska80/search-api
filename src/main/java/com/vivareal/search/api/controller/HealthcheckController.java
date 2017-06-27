package com.vivareal.search.api.controller;

import com.vivareal.search.api.model.Healthcheck;

import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthcheck")
public class HealthcheckController {

    @Autowired
    private TransportClient client;

    @Value("${es.controller.healthcheck.timeout}")
    private Integer timeout;

    @RequestMapping("/status")
    public Healthcheck status() {
        return new Healthcheck(client, timeout);
    }
}
