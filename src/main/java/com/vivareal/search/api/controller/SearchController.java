package com.vivareal.search.api.controller;

import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.http.SearchApiResponseError;
import com.vivareal.search.api.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/v2")
@Api(value = "v2", description = "Search API")
public class SearchController {

    private static Logger LOG = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @RequestMapping(value = {"/{index}/{id:[a-z0-9\\-]+}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Search by index with id", notes = "Returns index by identifier")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully get by id"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 404, message = "Id not found on cluster"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<Object> id(BaseApiRequest request, @PathVariable String id) {
        try {
            Optional<Object> response = searchService.getById(request, id);
            if (response.isPresent())
                return new ResponseEntity<>(response.get(), OK);

            LOG.debug("ID {} not found on {} index", id, request.getIndex());
            return new ResponseEntity<>(NOT_FOUND);
        } catch (Exception ex) {
            return hasError(request, ex);
        }
    }

    @RequestMapping(value = "/{index}", method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Search documents", notes = "Returns query-based documents")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully get documents"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<Object> search(SearchApiRequest request) {
        try {
            return new ResponseEntity<>(searchService.search(request), OK);
        } catch (Exception ex) {
            return hasError(request, ex);
        }
    }

    private ResponseEntity<Object> hasError(final BaseApiRequest request, final Throwable t) {
        HttpStatus httpStatus = ((t instanceof IllegalArgumentException || getRootCause(t) instanceof IllegalArgumentException) ? BAD_REQUEST : INTERNAL_SERVER_ERROR);
        String errorMessage = getRootCauseMessage(t);
        LOG.error(errorMessage, t);
        return new ResponseEntity<>(new SearchApiResponseError(errorMessage, request.toString()), httpStatus);
    }

    @RequestMapping(value = "/{index}/stream", method = GET)
    @ApiIgnore
    public StreamingResponseBody stream(BaseApiRequest request, HttpServletResponse httpServletResponse) {
        httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
        return out -> searchService.stream(request, out);
    }
}
