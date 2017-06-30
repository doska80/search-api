package com.vivareal.search.api.controller.stream;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
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
    private QueryAdapter<SearchRequestBuilder> queryAdapter;

    @Value("${es.scroll.timeout}")
    private Integer scrollTimeout;

    @Value("${es.stream.size}")
    private Integer size;

    @Value("${es.controller.stream.timeout}")
    private Integer timeout;

    public void stream(SearchApiRequest request, OutputStream stream) {

        TimeValue keepAlive = new TimeValue(this.scrollTimeout);
        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        requestBuilder.setScroll(keepAlive).setSize(size);

        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, requestBuilder.get(),
                        scroll -> scroll.setScroll(keepAlive).execute().actionGet(timeout)), (SearchHit sh) -> BytesReference.toBytes(sh.getSourceRef()));
    }
}
