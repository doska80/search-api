package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_SCRIPT;
import static com.newrelic.api.agent.NewRelic.incrementCounter;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.fromString;
import static org.elasticsearch.search.sort.SortBuilders.*;
import static org.elasticsearch.search.sort.SortOrder.valueOf;

import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.query.GeoPointItem;
import com.grupozap.search.api.model.query.GeoPointValue;
import com.grupozap.search.api.model.query.Item;
import com.grupozap.search.api.model.search.Sortable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

  private static final String NEW_RELIC_SORT_ERROR = "Custom/SearchAPI/v2/count/sort/error";

  private final SortParser sortParser;
  private final FilterQueryAdapter filterQueryAdapter;
  private final ScriptRemotePropertiesListener scriptRemotePropertiesListener;
  private final ElasticsearchSettingsAdapter elasticsearchSettingsAdapter;

  public SortQueryAdapter(
      SortParser sortParser,
      FilterQueryAdapter filterQueryAdapter,
      ScriptRemotePropertiesListener scriptRemotePropertiesListener,
      ElasticsearchSettingsAdapter elasticsearchSettingsAdapter) {

    this.sortParser = sortParser;
    this.filterQueryAdapter = filterQueryAdapter;
    this.scriptRemotePropertiesListener = scriptRemotePropertiesListener;
    this.elasticsearchSettingsAdapter = elasticsearchSettingsAdapter;
  }

  public void apply(SearchSourceBuilder searchSourceBuilder, final Sortable request) {
    if (!isBlank(request.getSort())) {
      try {
        sortParser
            .parse(request.getSort())
            .stream()
            .map(item -> asFieldSortBuilder(item, request))
            .filter(Objects::nonNull)
            .forEach(searchSourceBuilder::sort);
      } catch (ParserException | InvalidFieldException e) {
        if (isInvalidFieldException(e)) {
          LOG.warn(e.getMessage());
          incrementCounter(NEW_RELIC_SORT_ERROR);
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

  private SortBuilder asFieldSortBuilder(Item item, final Sortable request)
      throws InvalidFieldException {
    String fieldName = item.getField().getName();

    if (fieldName.equals("_score")) return scoreSort();

    if (FIELD_TYPE_SCRIPT.typeOf(
        elasticsearchSettingsAdapter.getFieldType(request.getIndex(), fieldName))) {

      Optional<ScriptRemotePropertiesListener.ScriptField> script =
          this.scriptRemotePropertiesListener
              .getScripts()
              .get(request.getIndex())
              .stream()
              .filter(scp -> fieldName.equals(scp.getId()))
              .findFirst();

      return script
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
          .orElse(null);
    }

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
    NestedSortBuilder nestedSortBuilder = new NestedSortBuilder(item.getField().firstName());

    item.getQueryFragment()
        .ifPresent(
            qf -> {
              BoolQueryBuilder queryBuilder = boolQuery();
              filterQueryAdapter.apply(queryBuilder, qf, request.getIndex(), new HashMap<>(), true);
              nestedSortBuilder.setFilter(queryBuilder);
            });

    return nestedSortBuilder;
  }
}
