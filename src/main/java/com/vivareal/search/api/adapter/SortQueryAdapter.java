package com.vivareal.search.api.adapter;

import static com.newrelic.api.agent.NewRelic.incrementCounter;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_SORT_DISABLE;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static java.lang.Boolean.TRUE;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.elasticsearch.search.sort.SortBuilders.geoDistanceSort;
import static org.elasticsearch.search.sort.SortBuilders.scoreSort;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.elasticsearch.search.sort.SortOrder.valueOf;

import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.GeoPointItem;
import com.vivareal.search.api.model.query.GeoPointValue;
import com.vivareal.search.api.model.query.Item;
import com.vivareal.search.api.model.search.Sortable;
import java.util.HashMap;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.jparsec.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SortQueryAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(SortQueryAdapter.class);

  private static final FieldSortBuilder DEFAULT_TIEBREAKER = fieldSort("_id").order(DESC);

  private static final String NEW_RELIC_USE_DEFAULT_SORT_METRIC =
      "Custom/SearchAPI/v2/count/default/sort";

  private final SortParser sortParser;
  private final FilterQueryAdapter filterQueryAdapter;

  public SortQueryAdapter(SortParser sortParser, FilterQueryAdapter filterQueryAdapter) {
    this.sortParser = sortParser;
    this.filterQueryAdapter = filterQueryAdapter;
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Sortable request) {
    if (TRUE.equals(ES_SORT_DISABLE.getValue(request.isDisableSort(), request.getIndex()))) return;

    if (!isBlank(request.getSort())) {
      applySortFromRequest(searchRequestBuilder, request);
    } else {
      applyDefaultSort(searchRequestBuilder, request);
    }
    searchRequestBuilder.addSort(DEFAULT_TIEBREAKER);
  }

  private void applySortFromRequest(SearchRequestBuilder searchRequestBuilder, Sortable request) {
    try {
      apply(
          searchRequestBuilder,
          request,
          request.getIndex(),
          ES_DEFAULT_SORT.getValue(request.getSort(), request.getIndex()));
    } catch (ParserException | InvalidFieldException e) {
      if (isInvalidFieldException(e)) {
        LOG.warn(e.getMessage());
        incrementCounter(NEW_RELIC_USE_DEFAULT_SORT_METRIC);
        applyDefaultSort(searchRequestBuilder, request);
      } else {
        throw e;
      }
    }
  }

  private boolean isInvalidFieldException(Exception e) {
    return e instanceof InvalidFieldException
        || (e.getCause() != null && e.getCause() instanceof InvalidFieldException);
  }

  private void apply(
      SearchRequestBuilder searchRequestBuilder,
      final Sortable request,
      final String index,
      final String sort) {
    sortParser
        .parse(sort)
        .stream()
        .map(item -> asFieldSortBuilder(index, item, request))
        .forEach(searchRequestBuilder::addSort);
  }

  private void applyDefaultSort(SearchRequestBuilder searchRequestBuilder, final Sortable request) {
    if (searchRequestBuilder.request().source() != null
        && searchRequestBuilder.request().source().sorts() != null)
      searchRequestBuilder.request().source().sorts().clear();

    apply(
        searchRequestBuilder,
        request,
        request.getIndex(),
        ES_DEFAULT_SORT.getValue(null, request.getIndex()));
  }

  private SortBuilder asFieldSortBuilder(String index, Item item, final Sortable request)
      throws InvalidFieldException {
    String fieldName = item.getField().getName();

    if (fieldName.equals("_score")) return scoreSort();

    if (item instanceof GeoPointItem) {
      GeoDistanceSortBuilder geoDistanceSortBuilder =
          getGeoDistanceSortBuilder((GeoPointItem) item);

      if (FIELD_TYPE_NESTED.typeOf(item.getField().getTypeFirstName()))
        geoDistanceSortBuilder.setNestedSort(getQueryFragment(item, request));

      return geoDistanceSortBuilder;
    }

    FieldSortBuilder fieldSortBuilder =
        fieldSort(fieldName).order(valueOf(item.getOrderOperator().name()));

    if (FIELD_TYPE_NESTED.typeOf(item.getField().getTypeFirstName()))
      fieldSortBuilder.setNestedSort(getQueryFragment(item, request));

    return fieldSortBuilder;
  }

  private GeoDistanceSortBuilder getGeoDistanceSortBuilder(GeoPointItem item) {
    GeoPointValue geoPointValue = item.getGeoPointValue();
    GeoPoint geoPoint = new GeoPoint(geoPointValue.value(0, 1), geoPointValue.value(0, 0));

    return geoDistanceSort(item.getField().getName(), geoPoint);
  }

  private NestedSortBuilder getQueryFragment(Item item, Sortable request) {
    {
      NestedSortBuilder nestedSortBuilder = new NestedSortBuilder(item.getField().firstName());

      item.getQueryFragment()
          .ifPresent(
              qf -> {
                BoolQueryBuilder queryBuilder = boolQuery();
                filterQueryAdapter.apply(
                    queryBuilder, qf, request.getIndex(), new HashMap<>(), true);
                nestedSortBuilder.setFilter(queryBuilder);
              });

      return nestedSortBuilder;
    }
  }
}
