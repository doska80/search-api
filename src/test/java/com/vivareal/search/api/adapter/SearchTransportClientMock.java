package com.vivareal.search.api.adapter;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.transport.MockTransportClient;
import org.junit.runner.RunWith;

import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.*;
import static org.elasticsearch.common.settings.Settings.EMPTY;

@SuppressWarnings("unchecked")
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class SearchTransportClientMock {

    protected ComplexRequestBuilder fullRequest = create().index(INDEX_NAME).from(0).size(20);

    protected BasicRequestBuilder basicRequest = basic().index(INDEX_NAME);

    protected FilterableRequestBuilder filterableRequest = filterable().index(INDEX_NAME);

    protected TransportClient transportClient = new MockTransportClient(EMPTY);

}
