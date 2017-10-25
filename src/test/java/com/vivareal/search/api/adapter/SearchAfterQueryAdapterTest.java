package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Test;

import static com.vivareal.search.api.adapter.SearchAfterQueryAdapter.SORT_SEPARATOR;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SearchAfterQueryAdapterTest extends SearchTransportClientMock {

    private final SearchAfterQueryAdapter searchAfterQueryAdapter = new SearchAfterQueryAdapter();

    @Test
    public void shouldApplySearchAfterAndSetFromToZeroWhenFieldUsedInSortIsValid() {
        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        requestBuilder.setFrom(20);

        SearchApiRequest request = fullRequest.build();
        String _uid = INDEX_NAME + "#1028071465";
        request.setCursorId("2.765432" + SORT_SEPARATOR + "A%5fB" + SORT_SEPARATOR + _uid);

        searchAfterQueryAdapter.apply(requestBuilder, request);
        assertNotNull(requestBuilder.request().source().searchAfter());
        assertEquals(0, requestBuilder.request().source().from());
        assertEquals("2.765432", requestBuilder.request().source().searchAfter()[0]);
        assertEquals("A_B", requestBuilder.request().source().searchAfter()[1]);
        assertEquals(_uid, requestBuilder.request().source().searchAfter()[2]);
    }

}
