package com.vivareal.search.api.controller.v2.stream;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

@Component
public class ElasticSearchStream {

    @Autowired
    private TransportClient client;

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter queryAdapter;

    @Value("${es.scroll.timeout}")
    private Integer scrollTimeout;

    @Value("${es.stream.size}")
    private Integer size;

    public void stream(SearchApiRequest request, OutputStream stream) {

        TimeValue timeout = new TimeValue(scrollTimeout);

        SearchRequestBuilder requestBuilder = (SearchRequestBuilder) this.queryAdapter.query(request);
        requestBuilder.setScroll(timeout);

        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, requestBuilder.get(),
                        scroll -> scroll.setScroll(timeout).execute().actionGet()), SearchHit::source);
    }
}
