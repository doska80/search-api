package com.vivareal.search.api.controller.stream;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;

@Component
public class ElasticSearchStream {

    @Autowired
    private TransportClient client;

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<?, SearchRequestBuilder> queryAdapter;

    public void stream(FilterableApiRequest request, OutputStream stream) {
        String index = request.getIndex();
        int scrollTimeout = ES_SCROLL_TIMEOUT.getValue(index);
        TimeValue keepAlive = new TimeValue(scrollTimeout);

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        requestBuilder.setScroll(keepAlive).setSize(ES_STREAM_SIZE.getValue(index));

        int streamTimeout = ES_CONTROLLER_STREAM_TIMEOUT.getValue(index);
        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, requestBuilder.get(),
                        scroll -> scroll.setScroll(keepAlive).execute().actionGet(streamTimeout)), (SearchHit sh) -> BytesReference.toBytes(sh.getSourceRef()));
    }
}
