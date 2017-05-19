package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.Healthcheck;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthcheck")
public class HealthcheckController {

    @RequestMapping("/status")
    public Healthcheck status() {
        return new Healthcheck(true);
    }
}