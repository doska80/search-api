package com.vivareal.search.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
//@Import({ApiBeans.class, ServiceConfig.class})
@ComponentScan("com.vivareal.search.api") // FIXME list all packages that have Spring components/beans to reduce boot time
@EnableAutoConfiguration
public class SearchAPI implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    public static void main(String[] args) {
        SpringApplication.run(SearchAPI.class, args);
    }
}
