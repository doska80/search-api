package com.vivareal.search.api.controller.stream;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.ActionRequestBuilder;
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

import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.ES_CONTROLLER_STREAM_TIMEOUT;
import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.ES_SCROLL_TIMEOUT;
import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.ES_STREAM_SIZE;
import static java.lang.Integer.parseInt;

@Component
public class ElasticSearchStream {

    @Autowired
    private TransportClient client;

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<?, SearchRequestBuilder> queryAdapter;

    public void stream(SearchApiRequest request, OutputStream stream) {

        TimeValue keepAlive = new TimeValue(parseInt(ES_SCROLL_TIMEOUT.getValue()));
        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        requestBuilder.setScroll(keepAlive).setSize(parseInt(ES_STREAM_SIZE.getValue()));

        int timeout = parseInt(ES_CONTROLLER_STREAM_TIMEOUT.getValue());

        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, requestBuilder.get(),
                        scroll -> scroll.setScroll(keepAlive).execute().actionGet(timeout)), (SearchHit sh) -> BytesReference.toBytes(sh.getSourceRef()));
    }
}
