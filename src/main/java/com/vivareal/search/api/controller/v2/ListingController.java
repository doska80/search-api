package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.controller.v2.stream.ResponseStream;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import com.vivareal.search.api.service.ListingService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping({"/v2/listing", "/v2/listings"})
public class ListingController {

    @Autowired
    private TransportClient client;

    @Autowired
    private ListingService listingService;

    @GetMapping("/{id:[a-z0-9\\-]+}")
    public SearchApiResponse getListingById(SearchApiRequest request, @PathVariable String id) { // FIXME accept request for non-filter params
        SearchApiResponse searchApiResponse = new SearchApiResponse();
        searchApiResponse.addListing(listingService.getListingById(request, id));
        return searchApiResponse;
    }

    @RequestMapping
    public SearchApiResponse getListings(SearchApiRequest request) {
        return new SearchApiResponse(listingService.query(request));
    }

//
//    @RequestMapping("/stream-spring")
//    public StreamingResponseBody streamSpring(SearchApiRequest request, HttpServletResponse httpResponse) throws IOException {
//        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
//        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(request.getQ());
//        boolQuery.must().add(queryString);
//
//        SearchRequestBuilder core = client.prepareSearch("inmuebles")
//                .setSize(100) // TODO we must configure timeouts
//                .setScroll(new TimeValue(60000));
//        core.setQuery(boolQuery);
//
//        SearchResponse response = core.get();
//
//        httpResponse.setContentType("application/x-ndjson");
//
//        return out -> ResponseStream.create(out)
//                .withIterator(new SearchApiIterator<>(client, response), SearchHit::source);
//    }
}
