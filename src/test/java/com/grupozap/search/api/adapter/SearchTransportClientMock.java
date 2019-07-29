package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.*;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@SuppressWarnings("unchecked")
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SearchTransportClientMock {

  protected final ComplexRequestBuilder fullRequest = create().index(INDEX_NAME).from(0).size(20);

  protected final BasicRequestBuilder basicRequest = basic().index(INDEX_NAME);

  protected final BasicRequestBuilder basicRequestWithIndexAlias = basic().index(INDEX_ALIAS_NAME);

  protected final FilterableRequestBuilder filterableRequest = filterable().index(INDEX_NAME);
}
