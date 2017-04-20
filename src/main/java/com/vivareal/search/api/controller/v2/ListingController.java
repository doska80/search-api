package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.controller.v2.stream.ResponseStream;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;

@RestController
@RequestMapping("/v2/listings")
public class ListingController {

    @Autowired
    private TransportClient client;

    @RequestMapping
    public SearchApiResponse getListings(SearchApiRequest request) {

        SearchResponse searchResponse = getSearchResponse(request);

        SearchApiResponse searchApiResponse = new SearchApiResponse();

        Stream.of(searchResponse.getHits().getHits()).forEach(doc -> searchApiResponse.addList(doc.getSource()));

        return searchApiResponse;
    }

    @RequestMapping("/stream")
    public void stream(SearchApiRequest request, HttpServletResponse httpResponse) throws IOException {
        SearchResponse response = getSearchResponse(request);

        ResponseStream.create(httpResponse.getOutputStream())
                .withIterator(new SearchApiIterator<>(client, response), SearchHit::source);
    }

    private SearchResponse getSearchResponse(SearchApiRequest request) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(request.getQ());
        boolQuery.must().add(queryString);

        SearchRequestBuilder core = client.prepareSearch("inmuebles")
                .setSize(100) // TODO we must configure timeouts
                .setScroll(new TimeValue(60000));
        core.setQuery(boolQuery);

        return core.get();
    }
}
