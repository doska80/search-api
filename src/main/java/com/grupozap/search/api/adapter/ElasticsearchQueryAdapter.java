package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_VALUE;
import static com.grupozap.search.api.model.http.DefaultFilterMode.ENABLED;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.cluster.routing.Preference.REPLICA_FIRST;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.query.QueryFragment;
import com.grupozap.search.api.service.parser.factory.DefaultFilterFactory;
import com.newrelic.api.agent.Trace;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("searchApiEnv")
public class ElasticsearchQueryAdapter implements QueryAdapter<GetRequest, SearchRequest> {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

  private final SourceFieldAdapter sourceFieldAdapter;
  private final PageQueryAdapter pageQueryAdapter;
  private final SearchAfterQueryAdapter searchAfterQueryAdapter;
  private final SortQueryAdapter sortQueryAdapter;
  private final QueryStringAdapter queryStringAdapter;
  private final FunctionScoreAdapter functionScoreAdapter;

  private final QueryParser queryParser;
  private final FilterQueryAdapter filterQueryAdapter;
  private final DefaultFilterFactory defaultFilterFactory;
  private final FacetQueryAdapter facetQueryAdapter;

  @Autowired
  public ElasticsearchQueryAdapter(
      SourceFieldAdapter sourceFieldAdapter,
      PageQueryAdapter pageQueryAdapter,
      SearchAfterQueryAdapter searchAfterQueryAdapter,
      SortQueryAdapter sortQueryAdapter,
      QueryStringAdapter queryStringAdapter,
      FunctionScoreAdapter functionScoreAdapter,
      QueryParser queryParser,
      FilterQueryAdapter filterQueryAdapter,
      DefaultFilterFactory defaultFilterFactory,
      FacetQueryAdapter facetQueryAdapter) {
    this.sourceFieldAdapter = sourceFieldAdapter;
    this.pageQueryAdapter = pageQueryAdapter;
    this.searchAfterQueryAdapter = searchAfterQueryAdapter;
    this.sortQueryAdapter = sortQueryAdapter;
    this.queryStringAdapter = queryStringAdapter;
    this.functionScoreAdapter = functionScoreAdapter;
    this.queryParser = queryParser;
    this.filterQueryAdapter = filterQueryAdapter;
    this.defaultFilterFactory = defaultFilterFactory;
    this.facetQueryAdapter = facetQueryAdapter;
  }

  @Override
  @Trace
  public GetRequest getById(BaseApiRequest request, String id) {
    final GetRequest getRequest = new GetRequest(request.getIndex(), request.getIndex(), id);
    sourceFieldAdapter.apply(getRequest, request);
    LOG.debug("Query getById {}", getRequest);
    return getRequest;
  }

  @Override
  @Trace
  public SearchRequest query(FilterableApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder));
  }

  @Override
  @Trace
  public SearchRequest query(SearchApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryBySearchApiRequest(request, searchBuilder, queryBuilder));
  }

  private SearchRequest prepareQuery(
      BaseApiRequest request, BiConsumer<SearchSourceBuilder, BoolQueryBuilder> builder) {

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.preference(REPLICA_FIRST.type());
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.timeout(
        new TimeValue(
            ES_QUERY_TIMEOUT_VALUE.getValue(request.getIndex()),
            TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex()))));

    searchRequest.source(searchSourceBuilder);
    searchRequest.indices(request.getIndex());

    BoolQueryBuilder queryBuilder = boolQuery();
    builder.accept(searchSourceBuilder, queryBuilder);

    if (searchSourceBuilder.query() == null) searchSourceBuilder.query(queryBuilder);

    LOG.debug("Request: {} - Builder: {}", request, searchSourceBuilder);
    return searchRequest;
  }

  private void buildQueryByFilterableApiRequest(
      FilterableApiRequest request,
      SearchSourceBuilder searchBuilder,
      BoolQueryBuilder queryBuilder) {
    pageQueryAdapter.apply(searchBuilder, request);
    sourceFieldAdapter.apply(searchBuilder, request);
    queryStringAdapter.apply(queryBuilder, request);
    functionScoreAdapter.apply(searchBuilder, queryBuilder, request);
    applyFilter(request, queryBuilder);
    sortQueryAdapter.apply(searchBuilder, request);
    searchAfterQueryAdapter.apply(searchBuilder, request);
  }

  private void applyFilter(FilterableApiRequest filterable, BoolQueryBuilder queryBuilder) {
    Optional<QueryFragment> requestFilter =
        ofNullable(filterable.getFilter()).filter(StringUtils::isNotEmpty).map(queryParser::parse);
    requestFilter.ifPresent(
        filter -> filterQueryAdapter.apply(queryBuilder, filter, filterable.getIndex()));

    if (ENABLED.equals(filterable.getDefaultFilterMode())) {
      Set<String> requestFields =
          requestFilter.map(qf -> qf.getFieldNames(false)).orElseGet(HashSet::new);
      defaultFilterFactory
          .getDefaultFilters(filterable.getIndex(), requestFields)
          .forEach(applyDefaultFilter(queryBuilder));
    }
  }

  private Consumer<BoolQueryBuilder> applyDefaultFilter(BoolQueryBuilder queryBuilder) {
    return defaultFilter -> {
      queryBuilder.filter().addAll(defaultFilter.filter());
      queryBuilder.mustNot().addAll(defaultFilter.mustNot());
      queryBuilder.must().addAll(defaultFilter.must());
      queryBuilder.should().addAll(defaultFilter.should());
    };
  }

  private void buildQueryBySearchApiRequest(
      SearchApiRequest request, SearchSourceBuilder searchBuilder, BoolQueryBuilder queryBuilder) {
    buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder);
    facetQueryAdapter.apply(searchBuilder, request);
  }
}
