package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_VALUE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
@DependsOn("searchApiEnv")
public class ElasticsearchQueryAdapter
    implements QueryAdapter<GetRequestBuilder, SearchRequestBuilder> {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

  private final ESClient esClient;

  private final SourceFieldAdapter sourceFieldAdapter;
  private final PageQueryAdapter pageQueryAdapter;
  private final SearchAfterQueryAdapter searchAfterQueryAdapter;
  private final SortQueryAdapter sortQueryAdapter;
  private final QueryStringAdapter queryStringAdapter;
  private final FunctionScoreAdapter functionScoreAdapter;
  private final FilterQueryAdapter filterQueryAdapter;
  private final FacetQueryAdapter facetQueryAdapter;

  @Autowired
  public ElasticsearchQueryAdapter(
      ESClient esClient,
      SourceFieldAdapter sourceFieldAdapter,
      PageQueryAdapter pageQueryAdapter,
      SearchAfterQueryAdapter searchAfterQueryAdapter,
      SortQueryAdapter sortQueryAdapter,
      QueryStringAdapter queryStringAdapter,
      FunctionScoreAdapter functionScoreAdapter,
      FilterQueryAdapter filterQueryAdapter,
      FacetQueryAdapter facetQueryAdapter) {
    this.esClient = esClient;
    this.sourceFieldAdapter = sourceFieldAdapter;
    this.pageQueryAdapter = pageQueryAdapter;
    this.searchAfterQueryAdapter = searchAfterQueryAdapter;
    this.sortQueryAdapter = sortQueryAdapter;
    this.queryStringAdapter = queryStringAdapter;
    this.functionScoreAdapter = functionScoreAdapter;
    this.filterQueryAdapter = filterQueryAdapter;
    this.facetQueryAdapter = facetQueryAdapter;
  }

  @Override
  @Trace
  public GetRequestBuilder getById(BaseApiRequest request, String id) {
    GetRequestBuilder requestBuilder =
        esClient.prepareGet(request, id).setRealtime(false).setOperationThreaded(false);

    sourceFieldAdapter.apply(requestBuilder, request);

    LOG.debug("Query getById {}", requestBuilder.request());

    return requestBuilder;
  }

  @Override
  @Trace
  public SearchRequestBuilder query(FilterableApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder));
  }

  @Override
  @Trace
  public SearchRequestBuilder query(SearchApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryBySearchApiRequest(request, searchBuilder, queryBuilder));
  }

  private SearchRequestBuilder prepareQuery(
      BaseApiRequest request, BiConsumer<SearchRequestBuilder, BoolQueryBuilder> builder) {
    SearchRequestBuilder searchBuilder =
        esClient
            .prepareSearch(request)
            .setTimeout(
                new TimeValue(
                    ES_QUERY_TIMEOUT_VALUE.getValue(request.getIndex()),
                    TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex()))));

    BoolQueryBuilder queryBuilder = boolQuery();
    builder.accept(searchBuilder, queryBuilder);

    if (searchBuilder.request().source().query() == null) searchBuilder.setQuery(queryBuilder);

    LOG.debug("Request: {} - Query: {}", request, searchBuilder);
    return searchBuilder;
  }

  private void buildQueryByFilterableApiRequest(
      FilterableApiRequest request,
      SearchRequestBuilder searchBuilder,
      BoolQueryBuilder queryBuilder) {
    pageQueryAdapter.apply(searchBuilder, request);
    sourceFieldAdapter.apply(searchBuilder, request);
    queryStringAdapter.apply(queryBuilder, request);
    functionScoreAdapter.apply(searchBuilder, queryBuilder, request);
    filterQueryAdapter.apply(queryBuilder, request);
    sortQueryAdapter.apply(searchBuilder, request);
    searchAfterQueryAdapter.apply(searchBuilder, request);
  }

  private void buildQueryBySearchApiRequest(
      SearchApiRequest request, SearchRequestBuilder searchBuilder, BoolQueryBuilder queryBuilder) {
    buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder);
    facetQueryAdapter.apply(searchBuilder, request);
  }
}
