package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import com.vivareal.search.api.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping({"/v2", "/v2"})
public class SearchController {

    @Autowired
    private SearchService searchService;

    @RequestMapping(value = {"/{index}/{id:[a-z0-9\\-]+}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SearchApiResponse getById(@PathVariable("index") String index, SearchApiRequest request, @PathVariable String id) { // FIXME accept request for non-filter params
        SearchApiResponse searchApiResponse = SearchApiResponse.builder();
        request.setIndex(index);
        return searchService.getById(request, id).orElse(searchApiResponse);
    }

    @RequestMapping(value = {"/{index}"}, method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SearchApiResponse search(@PathVariable("index") String index, SearchApiRequest request) {
        request.setIndex(index);
        return searchService.search(request);
    }

    @RequestMapping(value = "/{index}/stream", method = GET)
    public StreamingResponseBody stream(@PathVariable("index") String index, SearchApiRequest request, HttpServletResponse httpServletResponse) {
        request.setIndex(index);
        httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
        return out -> searchService.stream(request, out);
    }
}
