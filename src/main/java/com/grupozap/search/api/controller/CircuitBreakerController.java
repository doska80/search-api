package com.grupozap.search.api.controller;

import static java.util.Map.of;
import static org.springframework.http.ResponseEntity.ok;

import com.grupozap.search.api.service.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/circuits")
@Api("v2")
public class CircuitBreakerController {

  @Autowired private CircuitBreakerService service;

  @GetMapping("/")
  public ResponseEntity<?> list() {
    return ok().body(of("circuits", service.list()));
  }

  @GetMapping("/actions/state/{state}")
  public ResponseEntity<Object> modifyAll(@PathVariable State state) {
    service.modifyAll(state);
    return ok().build();
  }
}
