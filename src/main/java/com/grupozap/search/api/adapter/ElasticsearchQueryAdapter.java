package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_VALUE;
import static com.grupozap.search.api.model.http.DefaultFilterMode.ENABLED;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.service.parser.factory.DefaultFilterFactory;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("searchApiEnv")
public class ElasticsearchQueryAdapter implements QueryAdapter<GetRequest, SearchRequest> {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

  private final SourceFieldAdapter sourceFieldAdapter;
  private final PageQueryAdapter pageQueryAdapter;
  private final SortQueryAdapter sortQueryAdapter;
  private final QueryStringAdapter queryStringAdapter;
  private final FunctionScoreAdapter functionScoreAdapter;

  private final QueryParser queryParser;
  private final FilterQueryAdapter filterQueryAdapter;
  private final DefaultFilterFactory defaultFilterFactory;
  private final FacetQueryAdapter facetQueryAdapter;

  private final boolean realtimeEnabled;

  @Autowired
  public ElasticsearchQueryAdapter(
      SourceFieldAdapter sourceFieldAdapter,
      PageQueryAdapter pageQueryAdapter,
      SortQueryAdapter sortQueryAdapter,
      QueryStringAdapter queryStringAdapter,
      FunctionScoreAdapter functionScoreAdapter,
      QueryParser queryParser,
      FilterQueryAdapter filterQueryAdapter,
      DefaultFilterFactory defaultFilterFactory,
      FacetQueryAdapter facetQueryAdapter,
      @Value("${es.get.by.id.realtime.enabled:false}") boolean realtimeEnabled) {
    this.sourceFieldAdapter = sourceFieldAdapter;
    this.pageQueryAdapter = pageQueryAdapter;
    this.sortQueryAdapter = sortQueryAdapter;
    this.queryStringAdapter = queryStringAdapter;
    this.functionScoreAdapter = functionScoreAdapter;
    this.queryParser = queryParser;
    this.filterQueryAdapter = filterQueryAdapter;
    this.defaultFilterFactory = defaultFilterFactory;
    this.facetQueryAdapter = facetQueryAdapter;
    this.realtimeEnabled = realtimeEnabled;
  }

  @Override
  public GetRequest getById(BaseApiRequest request, String id) {
    final var getRequest = new GetRequest(request.getIndex(), id).realtime(realtimeEnabled);
    sourceFieldAdapter.apply(getRequest, request);
    LOG.debug("Query getById {}", getRequest);
    return getRequest;
  }

  @Override
  public SearchRequest query(FilterableApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder));
  }

  @Override
  public SearchRequest query(SearchApiRequest request) {
    return prepareQuery(
        request,
        (searchBuilder, queryBuilder) ->
            buildQueryBySearchApiRequest(request, searchBuilder, queryBuilder));
  }

  private SearchRequest prepareQuery(
      BaseApiRequest request, BiConsumer<SearchSourceBuilder, BoolQueryBuilder> builder) {

    var searchRequest = new SearchRequest();
    var searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.trackTotalHits(true);
    searchSourceBuilder.timeout(
        new TimeValue(
            ES_QUERY_TIMEOUT_VALUE.getValue(request.getIndex()),
            TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex()))));

    searchRequest.source(searchSourceBuilder);
    searchRequest.indices(request.getIndex());

    var queryBuilder = boolQuery();
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
  }

  private void applyFilter(FilterableApiRequest filterable, BoolQueryBuilder esQueryBuilder) {
    var requestedFiltersQueryBuilder = boolQuery();

    // Reference to query builder to be applied on esQueryBuilder
    var rootQueryBuilder = requestedFiltersQueryBuilder;

    var requestFilter =
        ofNullable(filterable.getFilter()).filter(StringUtils::isNotEmpty).map(queryParser::parse);
    requestFilter.ifPresent(
        filter ->
            filterQueryAdapter.apply(requestedFiltersQueryBuilder, filter, filterable.getIndex()));

    if (ENABLED.equals(filterable.getDefaultFilterMode())) {
      var requestFields = requestFilter.map(qf -> qf.getFieldNames(false)).orElseGet(HashSet::new);
      var defaultFilters =
          defaultFilterFactory.getDefaultFilters(filterable.getIndex(), requestFields);

      if (!defaultFilters.isEmpty()) {
        var wrapperFilterQueryBuilder = boolQuery();
        if (isNotBlank(filterable.getFilter())) {
          wrapperFilterQueryBuilder.filter().add(requestedFiltersQueryBuilder);
        }
        defaultFilters.forEach(df -> wrapperFilterQueryBuilder.filter().add(df));
        rootQueryBuilder = wrapperFilterQueryBuilder;
      }
    }

    // Apply root filter query builder clauses on elastic search query builder
    esQueryBuilder.filter().addAll(rootQueryBuilder.filter());
    esQueryBuilder.mustNot().addAll(rootQueryBuilder.mustNot());
    esQueryBuilder.must().addAll(rootQueryBuilder.must());
    esQueryBuilder.should().addAll(rootQueryBuilder.should());
  }

  private void buildQueryBySearchApiRequest(
      SearchApiRequest request, SearchSourceBuilder searchBuilder, BoolQueryBuilder queryBuilder) {
    buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder);
    facetQueryAdapter.apply(searchBuilder, request);
  }
}
