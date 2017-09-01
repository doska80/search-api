package com.vivareal.search.api.adapter;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.vivareal.search.api.model.http.SearchApiRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.transport.MockTransportClient;
import org.junit.runner.RunWith;

import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.elasticsearch.common.settings.Settings.EMPTY;

@SuppressWarnings("unchecked")
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class SearchTransportClientMock {

    protected SearchApiRequestBuilder.ComplexRequestBuilder fullRequest = SearchApiRequestBuilder.create().index(INDEX_NAME).from(0).size(20);

    protected SearchApiRequestBuilder.BasicRequestBuilder basicRequest = fullRequest.basic();

    protected TransportClient transportClient = new MockTransportClient(EMPTY);

}
