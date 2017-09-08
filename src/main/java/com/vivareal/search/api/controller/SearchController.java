package com.vivareal.search.api.controller;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/v2")
@Api("v2")
@DefaultProperties(
    defaultFallback = "fallback",
    commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")
    },
    ignoreExceptions = {
        IllegalArgumentException.class
    }
)
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private Environment environment;

    @RequestMapping(value = {"/{index}/{id:[a-z0-9\\-]+}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Search by index with id", notes = "Returns index by identifier")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully get by id"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 404, message = "Id not found on cluster"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @HystrixCommand(
        commandProperties = {
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "100"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "90")
        }
    )
    public ResponseEntity<Object> id(BaseApiRequest request, @PathVariable String id) throws InterruptedException, ExecutionException, TimeoutException {
        return searchService.getById(request, id)
            .map(ResponseEntity::ok)
            .orElse(notFound().build());
    }

    @RequestMapping(value = "/{index}", method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Search documents", notes = "Returns query-based documents")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully get documents"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @HystrixCommand(
        commandProperties = {
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "30"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "70")
        }
    )
    public ResponseEntity<Object> search(SearchApiRequest request) {
        return new ResponseEntity<>(searchService.search(request), OK);
    }

    public ResponseEntity<Object> fallback(Throwable e) throws Throwable {
        throw e;
    }

    @RequestMapping(value = {"/forceOpen/{force}"}, method = GET)
    @ApiIgnore
    public ResponseEntity<Object> forceOpen(@PathVariable("force") boolean force) {
        if(Arrays.stream(environment.getActiveProfiles()).noneMatch(env -> env.equalsIgnoreCase("test")))
            return new ResponseEntity<>(NOT_FOUND);

        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.forceOpen", force);
        return new ResponseEntity<>(OK);
    }

    @RequestMapping(value = "/{index}/stream", method = GET)
    @ApiIgnore
    public StreamingResponseBody stream(BaseApiRequest request, HttpServletResponse httpServletResponse) {
        httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
        return out -> searchService.stream(request, out);
    }
}
