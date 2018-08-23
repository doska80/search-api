package com.grupozap.search.api.adapter;

import static com.vivareal.search.api.adapter.SearchAfterQueryAdapter.SORT_SEPARATOR;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequestBuilder;
import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

public class SearchAfterQueryAdapterTest extends SearchTransportClientMock {

  private final SearchAfterQueryAdapter searchAfterQueryAdapter = new SearchAfterQueryAdapter();

  @Test
  public void shouldApplySearchAfterAndSetFromToZeroWhenFieldUsedInSortIsValid() {
    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    requestBuilder.from(20);

    SearchApiRequest request = fullRequest.build();
    String _id = SearchApiRequestBuilder.INDEX_NAME + "#1028071465";
    request.setCursorId("2.765432" + SearchAfterQueryAdapter.SORT_SEPARATOR + "A%5fB" + SearchAfterQueryAdapter.SORT_SEPARATOR + _id);

    searchAfterQueryAdapter.apply(requestBuilder, request);
    assertNotNull(requestBuilder.searchAfter());
    assertEquals(0, requestBuilder.from());
    assertEquals("2.765432", requestBuilder.searchAfter()[0]);
    assertEquals("A_B", requestBuilder.searchAfter()[1]);
    assertEquals(_id, requestBuilder.searchAfter()[2]);
  }
}
