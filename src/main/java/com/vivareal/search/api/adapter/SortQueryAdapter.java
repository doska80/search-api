package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.elasticsearch.search.sort.SortBuilders.scoreSort;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.elasticsearch.search.sort.SortOrder.valueOf;

import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.Sort.Item;
import com.vivareal.search.api.model.search.Sortable;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SortQueryAdapter {

  private static final FieldSortBuilder DEFAULT_TIEBREAKER = fieldSort("_uid").order(DESC);

  private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
  private final SortParser sortParser;
  private final FilterQueryAdapter filterQueryAdapter;

  public SortQueryAdapter(
      @Qualifier("elasticsearchSettings")
          SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter,
      SortParser sortParser,
      FilterQueryAdapter filterQueryAdapter) {
    this.settingsAdapter = settingsAdapter;
    this.sortParser = sortParser;
    this.filterQueryAdapter = filterQueryAdapter;
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Sortable request) {
    if (request.getSort() != null && "".equals(request.getSort().trim())) return;

    sortParser
        .parse(ES_DEFAULT_SORT.getValue(request.getSort(), request.getIndex()))
        .stream()
        .map(item -> asFieldSortBuilder(request.getIndex(), item, request))
        .forEach(searchRequestBuilder::addSort);
    searchRequestBuilder.addSort(DEFAULT_TIEBREAKER);
  }

  private SortBuilder asFieldSortBuilder(String index, Item item, final Sortable request) {
    String fieldName = item.getField().getName();

    if (fieldName.equals("_score")) return scoreSort();

    settingsAdapter.checkFieldName(index, fieldName, false);

    FieldSortBuilder fieldSortBuilder =
        fieldSort(fieldName).order(valueOf(item.getOrderOperator().name()));
    String parentField = fieldName.split("\\.")[0];

    if (settingsAdapter.isTypeOf(index, parentField, FIELD_TYPE_NESTED)) {
      fieldSortBuilder.setNestedPath(parentField);

      item.getQueryFragment()
          .ifPresent(
              qf -> {
                BoolQueryBuilder queryBuilder = boolQuery();
                filterQueryAdapter.apply(
                    queryBuilder, qf, request.getIndex(), new HashMap<>(), true);
                fieldSortBuilder.setNestedFilter(queryBuilder);
              });
    }

    return fieldSortBuilder;
  }
}
