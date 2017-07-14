package com.vivareal.search.api.controller;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import com.vivareal.search.api.model.SearchApiResponseError;
import com.vivareal.search.api.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping({"/v2", "/v2"})
public class SearchController {

    private static Logger LOG = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @RequestMapping(value = {"/{index}/{id:[a-z0-9\\-]+}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> getById(@PathVariable("index") String index, SearchApiRequest request, @PathVariable String id) {
        request.setIndex(index);
        try {
            Optional<SearchApiResponse> response = searchService.getById(request, id);
            if (response.isPresent())
                return new ResponseEntity<>(response.get(), OK);

            LOG.debug("ID {} not found on {} index", id, index);

            return new ResponseEntity<>(NOT_FOUND);

        } catch (IllegalArgumentException ex) {
            String errorMessage = getRootCauseMessage(ex);
            LOG.error(errorMessage);
            return new ResponseEntity<>(new SearchApiResponseError(errorMessage, request.toString()), BAD_REQUEST);
        }
    }

    @RequestMapping(value = {"/{index}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> search(@PathVariable("index") String index, SearchApiRequest request) {
        request.setIndex(index);
        try {
            return new ResponseEntity<>(searchService.search(request), OK);
        } catch (IllegalArgumentException ex) {
            String errorMessage = getRootCauseMessage(ex);
            LOG.error(errorMessage);
            return new ResponseEntity<>(new SearchApiResponseError(errorMessage, request.toString()), BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/{index}/stream", method = GET)
    public StreamingResponseBody stream(@PathVariable("index") String index, SearchApiRequest request, HttpServletResponse httpServletResponse) {
        request.setIndex(index);
        httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
        return out -> searchService.stream(request, out);
    }
}
