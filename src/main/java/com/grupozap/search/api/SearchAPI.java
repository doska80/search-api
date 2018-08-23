package com.grupozap.search.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.springframework.http.HttpStatus.*;
import static springfox.documentation.builders.PathSelectors.regex;

import com.grupozap.search.api.configuration.NewRelicTransactionFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.turbine.EnableTurbine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@Configuration
@EnableAsync
@ComponentScan(basePackages = {"com.grupozap.search.api"})
@EnableSwagger2
@EnableHystrix
@EnableHystrixDashboard
@EnableTurbine
public class SearchAPI implements WebMvcConfigurer {

  public static void main(String[] args) {
    SpringApplication.run(SearchAPI.class, args);
  }

  @Bean
  public FilterRegistrationBean newRelicFilter() {
    final FilterRegistrationBean registrationBean =
        new FilterRegistrationBean(new NewRelicTransactionFilter());
    registrationBean.addUrlPatterns("/*");

    return registrationBean;
  }

  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .paths(regex("/v2.*"))
        .build()
        .ignoredParameterTypes(ApiIgnore.class)
        .globalResponseMessage(
            RequestMethod.GET,
            newArrayList(
                statusCode(OK),
                statusCode(NOT_FOUND),
                statusCode(BAD_REQUEST),
                statusCode(INTERNAL_SERVER_ERROR)));
  }

  private ResponseMessage statusCode(HttpStatus status) {
    return new ResponseMessageBuilder()
        .code(status.value())
        .message(status.getReasonPhrase())
        .build();
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("Search API")
        .termsOfServiceUrl("https://github.com/grupozap/search-api")
        .version("2.0")
        .build();
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("redirect:/swagger-ui.html");
  }
}
