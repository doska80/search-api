package com.grupozap.search.api.adapter;

import static com.google.common.collect.Maps.newHashMap;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_DISABLE;
import static com.grupozap.search.api.model.mapping.MappingType.*;
import static java.lang.Boolean.FALSE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.fromString;
import static org.elasticsearch.search.sort.SortBuilders.*;
import static org.elasticsearch.search.sort.SortOrder.valueOf;

import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.exception.RescoreConjunctionSortException;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener;
import com.grupozap.search.api.listener.SortRescoreListener;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.query.GeoPointItem;
import com.grupozap.search.api.model.query.Item;
import com.grupozap.search.api.model.search.Sortable;
import com.grupozap.search.api.query.LtrQueryBuilder;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.rescore.QueryRescoreMode;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.jparsec.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SortQueryAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(SortQueryAdapter.class);

  private final SortParser sortParser;
  private final FilterQueryAdapter filterQueryAdapter;
  private final ScriptRemotePropertiesListener scriptRemotePropertiesListener;
  private final ElasticsearchSettingsAdapter elasticsearchSettingsAdapter;
  private final SortRescoreListener sortRescoreListener;

  public SortQueryAdapter(
      SortParser sortParser,
      FilterQueryAdapter filterQueryAdapter,
      ScriptRemotePropertiesListener scriptRemotePropertiesListener,
      ElasticsearchSettingsAdapter elasticsearchSettingsAdapter,
      SortRescoreListener sortRescoreListener) {

    this.sortParser = sortParser;
    this.filterQueryAdapter = filterQueryAdapter;
    this.scriptRemotePropertiesListener = scriptRemotePropertiesListener;
    this.elasticsearchSettingsAdapter = elasticsearchSettingsAdapter;
    this.sortRescoreListener = sortRescoreListener;
  }

  public void apply(SearchSourceBuilder searchSourceBuilder, final Sortable request) {
    if (FALSE.equals(ES_SORT_DISABLE.getValue(request.isDisableSort(), request.getIndex()))) {
      try {
        sortParser
            .parse(
                isBlank(request.getSort())
                    ? ES_DEFAULT_SORT.getValue(request.getIndex())
                    : request.getSort())
            .forEach(
                item -> {
                  validateRescoreCompoudBySort(searchSourceBuilder, request);
                  if (item.getField().getName().equals("_score")) {
                    searchSourceBuilder.sort(scoreSort());

                  } else if (FIELD_TYPE_RESCORE.typeOf(item.getField().getType())) {
                    applyRescoreQuery(searchSourceBuilder, request.getIndex(), item);

                  } else if (FIELD_TYPE_SCRIPT.typeOf(
                      elasticsearchSettingsAdapter.getFieldType(
                          request.getIndex(), item.getField().getName()))) {
                    applyScriptSortQuery(searchSourceBuilder, request.getIndex(), item);

                  } else if (item instanceof GeoPointItem) {
                    applyGeoDistanceSortBuilder(
                        searchSourceBuilder, request.getIndex(), (GeoPointItem) item);

                  } else {
                    var fieldSortBuilder =
                        fieldSort(item.getField().getName())
                            .order(valueOf(item.getOrderOperator().name()));
                    if (FIELD_TYPE_NESTED.typeOf(item.getField().getTypeFirstName()))
                      fieldSortBuilder.setNestedSort(getQueryFragment(item, request.getIndex()));
                    searchSourceBuilder.sort(fieldSortBuilder);
                  }
                });
      } catch (ParserException | InvalidFieldException e) {
        if (isInvalidFieldException(e)) {
          LOG.warn(e.getMessage());
        } else {
          throw e;
        }
      }
    }
  }

  private boolean isInvalidFieldException(Exception e) {
    return e instanceof InvalidFieldException
        || (e.getCause() != null && e.getCause() instanceof InvalidFieldException);
  }

  private void applyRescoreQuery(
      final SearchSourceBuilder searchSourceBuilder, final String index, final Item item) {
    var sortRescore = sortRescoreListener.getRescorerOrders(index).get(item.getField().getName());
    var queryRescorerBuilder =
        new QueryRescorerBuilder(
                new LtrQueryBuilder.Builder(sortRescore.getModel())
                    .params(sortRescore.getParams())
                    .activeFeatures(sortRescore.getActiveFeatures())
                    .build())
            .setScoreMode(QueryRescoreMode.fromString(sortRescore.getScoreMode()))
            .windowSize(sortRescore.getWindowSize())
            .setQueryWeight(sortRescore.getQueryWeight())
            .setRescoreQueryWeight(sortRescore.getRescoreQueryWeight());
    searchSourceBuilder.addRescorer(queryRescorerBuilder);
  }

  private void applyScriptSortQuery(
      final SearchSourceBuilder searchSourceBuilder, final String index, final Item item) {
    this.scriptRemotePropertiesListener.getScripts().get(index).stream()
        .filter(scp -> item.getField().getName().equals(scp.getId()))
        .findFirst()
        .<SortBuilder>map(
            scriptField ->
                scriptSort(
                        new Script(
                            ScriptType.valueOf(scriptField.getScriptType().toUpperCase()),
                            DEFAULT_SCRIPT_LANG.equalsIgnoreCase(scriptField.getLang())
                                ? null
                                : scriptField.getLang(),
                            scriptField.getId(),
                            scriptField.getParams()),
                        fromString(scriptField.getScriptSortType()))
                    .order(valueOf(item.getOrderOperator().name())))
        .ifPresent(searchSourceBuilder::sort);
  }

  private void applyGeoDistanceSortBuilder(
      final SearchSourceBuilder searchSourceBuilder, final String index, GeoPointItem item) {
    var geoPointValue = item.getGeoPointValue();
    var geoPoint = new GeoPoint(geoPointValue.value(0, 1), geoPointValue.value(0, 0));
    var geoDistanceSortBuilder = geoDistanceSort(item.getField().getName(), geoPoint);
    if (FIELD_TYPE_NESTED.typeOf(item.getField().getTypeFirstName()))
      geoDistanceSortBuilder.setNestedSort(this.getQueryFragment(item, index));
    searchSourceBuilder.sort(geoDistanceSortBuilder);
  }

  private NestedSortBuilder getQueryFragment(Item item, final String index) {
    var nestedSortBuilder = new NestedSortBuilder(item.getField().firstName());
    item.getQueryFragment()
        .ifPresent(
            qf -> {
              var queryBuilder = boolQuery();
              filterQueryAdapter.apply(queryBuilder, qf, index, newHashMap(), true);
              nestedSortBuilder.setFilter(queryBuilder);
            });
    return nestedSortBuilder;
  }

  private void validateRescoreCompoudBySort(
      SearchSourceBuilder searchSourceBuilder, final Sortable request) {
    if (!isEmpty(searchSourceBuilder.rescores()))
      throw new RescoreConjunctionSortException(request.getSort());
  }
}
