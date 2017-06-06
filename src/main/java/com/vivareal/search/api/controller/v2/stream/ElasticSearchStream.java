package com.vivareal.search.api.controller.v2.stream;

import com.vivareal.search.api.model.SearchApiIndex;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

import static java.util.Optional.ofNullable;

@Component
public class ElasticSearchStream {

    @Autowired
    private TransportClient client;

    @Value("${es.scroll.timeout}")
    private Integer scrollTimeout;

    public void stream(SearchApiRequest request, OutputStream stream) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(request.getQ());
        boolQuery.must().add(queryString);

        TimeValue timeout = new TimeValue(scrollTimeout);

        SearchRequestBuilder core = client.prepareSearch(SearchApiIndex.of(request).getIndex())
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setSize(ofNullable(request.getSize()).map(Integer::parseInt).orElse(10))
                .setFrom(ofNullable(request.getFrom()).map(Integer::parseInt).orElse(0))
                .setScroll(timeout);

        core.setQuery(boolQuery);

        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, core.get(),
                        (scroll) -> scroll.setScroll(timeout).execute().actionGet()), SearchHit::source);
    }
}