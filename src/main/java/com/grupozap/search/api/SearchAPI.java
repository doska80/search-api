package com.grupozap.search.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.grupozap.search.api"})
public class SearchAPI {

  public static void main(String[] args) {
    SpringApplication.run(SearchAPI.class, args);
  }
}
