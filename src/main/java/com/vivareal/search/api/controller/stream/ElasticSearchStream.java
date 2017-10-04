package com.vivareal.search.api.controller.stream;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.adapter.SettingsAdapter;
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
import java.util.Map;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.lang.String.valueOf;

@Component
public class ElasticSearchStream {

    @Autowired
    private TransportClient client;

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<?, SearchRequestBuilder> queryAdapter;

    @Autowired
    @Qualifier("elasticsearchSettings")
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    public void stream(FilterableApiRequest request, OutputStream stream) {
        String index = request.getIndex();
        int scrollTimeout = ES_SCROLL_TIMEOUT.getValue(index);
        TimeValue keepAlive = new TimeValue(scrollTimeout);

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        requestBuilder.setScroll(keepAlive).setSize(ES_STREAM_SIZE.getValue(index));

        Integer count = null;
        if(request.getSize() != Integer.MAX_VALUE && request.getSize() != 0) {
            final int shardSize = parseInt(valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS)));
            count = request.getSize() + shardSize;
            requestBuilder.setSize(min(request.getSize(), ES_STREAM_SIZE.getValue(index)));
            requestBuilder.setTerminateAfter((request.getSize() / shardSize) + ((request.getSize() % shardSize == 0) ? 0 : 1));
        }

        int streamTimeout = ES_CONTROLLER_STREAM_TIMEOUT.getValue(index);
        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(client, requestBuilder.get(),
                        scroll -> scroll.setScroll(keepAlive).execute().actionGet(streamTimeout), count), (SearchHit sh) -> BytesReference.toBytes(sh.getSourceRef()));
    }
}
