package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.UnsupportedFieldException;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.*;
import com.vivareal.search.api.model.search.Filterable;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vivareal.search.api.adapter.FilterQueryAdapter.QueryType.*;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_MAPPING_META_FIELDS_ID;
import static com.vivareal.search.api.model.mapping.MappingType.*;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Component
public class FilterQueryAdapter {

    private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
    private final QueryParser queryParser;

    public FilterQueryAdapter(@Qualifier("elasticsearchSettings") SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter,
                              QueryParser queryParser) {
        this.settingsAdapter = settingsAdapter;
        this.queryParser = queryParser;
    }

    private LogicalOperator getLogicalOperatorByQueryFragmentList(final QueryFragmentList queryFragmentList, int index, LogicalOperator logicalOperator) {
        if (index + 1 < queryFragmentList.size()) {
            QueryFragment next = queryFragmentList.get(index + 1);
            if (next instanceof QueryFragmentItem)
                return ((QueryFragmentItem) next).getLogicalOperator();

            if (next instanceof QueryFragmentOperator)
                return ((QueryFragmentOperator) next).getOperator();
        }
        return logicalOperator;
    }

    public void apply(BoolQueryBuilder queryBuilder, final Filterable filter) {
        ofNullable(filter.getFilter()).ifPresent(f -> apply(queryBuilder, queryParser.parse(f), filter.getIndex(), new HashMap<>(), false));
    }

    public void apply(BoolQueryBuilder queryBuilder, final QueryFragment queryFragment, final String indexName, Map<FilterQueryAdapter.QueryType, Map<String, BoolQueryBuilder>> nestedMap, boolean ignoreNestedQueryBuilder) {
        if (queryFragment == null || !(queryFragment instanceof QueryFragmentList))
            return;

        QueryFragmentList queryFragmentList = (QueryFragmentList) queryFragment;
        LogicalOperator logicalOperator = AND;

        for (int index = 0; index < queryFragmentList.size(); index++) {
            QueryFragment queryFragmentFilter = queryFragmentList.get(index);

            if (queryFragmentFilter instanceof QueryFragmentList) {
                BoolQueryBuilder recursiveQueryBuilder = boolQuery();
                logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);
                Map<FilterQueryAdapter.QueryType, Map<String, BoolQueryBuilder>> innerNestedMap = addFilterQuery(new HashMap<>(), queryBuilder, recursiveQueryBuilder, logicalOperator, isNotBeforeCurrentQueryFragment(queryFragmentList, index), false, null);
                apply(recursiveQueryBuilder, queryFragmentFilter, indexName, innerNestedMap, ignoreNestedQueryBuilder);

            } else if (queryFragmentFilter instanceof QueryFragmentItem) {
                QueryFragmentItem queryFragmentItem = (QueryFragmentItem) queryFragmentFilter;
                Filter filter = queryFragmentItem.getFilter();

                String fieldName = filter.getField().getName();
                String fieldFirstName = filter.getField().firstName();
                settingsAdapter.checkFieldName(indexName, fieldName, false);
                boolean nested = settingsAdapter.isTypeOf(indexName, fieldFirstName, FIELD_TYPE_NESTED) && !ignoreNestedQueryBuilder;

                final boolean not = isNotBeforeCurrentQueryFragment(queryFragmentList, index);
                logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);
                RelationalOperator operator = filter.getRelationalOperator();

                if (filter.getValue().isEmpty()) {
                    addFilterQuery(nestedMap, queryBuilder, existsQuery(fieldName), logicalOperator, DIFFERENT.equals(operator) == not, nested, fieldFirstName);
                    continue;
                }

                Value filterValue = filter.getValue();

                switch (operator) {
                    case DIFFERENT:
                        addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, filterValue.value()), logicalOperator, !not, nested, fieldFirstName);
                        break;

                    case EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, filterValue.value()), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case RANGE:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value(0)).to(filterValue.value(1)).includeLower(true).includeUpper(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case GREATER:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value()).includeLower(false), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case GREATER_EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value()).includeLower(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LESS:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).to(filterValue.value()).includeUpper(false), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LESS_EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).to(filterValue.value()).includeUpper(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LIKE:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_KEYWORD))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_KEYWORD.toString(), LIKE);

                        addFilterQuery(nestedMap, queryBuilder, wildcardQuery(fieldName, filter.getValue().first()), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case IN:
                        if (fieldName.equals(ES_MAPPING_META_FIELDS_ID.getValue(indexName))) {
                            String[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).map(Object::toString).toArray(String[]::new);
                            addFilterQuery(nestedMap, queryBuilder, idsQuery().addIds(values), logicalOperator, not, nested, fieldFirstName);
                        } else {
                            Object[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).toArray();
                            addFilterQuery(nestedMap, queryBuilder, termsQuery(fieldName, values), logicalOperator, not, nested, fieldFirstName);
                        }
                        break;

                    case POLYGON:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_GEOPOINT))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_GEOPOINT.toString(), POLYGON);

                        List<GeoPoint> points = filterValue
                        .stream()
                        .map(point -> new GeoPoint(((Value) point).<Double>value(1), ((Value) point).<Double>value(0)))
                        .collect(toList());

                        addFilterQuery(nestedMap, queryBuilder, geoPolygonQuery(fieldName, points), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case VIEWPORT:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_GEOPOINT))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_GEOPOINT.toString(), VIEWPORT);

                        GeoPoint topRight = new GeoPoint(filterValue.value(0, 1), filterValue.value(0, 0));
                        GeoPoint bottomLeft = new GeoPoint(filterValue.value(1, 1), filterValue.value(1, 0));

                        addFilterQuery(nestedMap, queryBuilder, geoBoundingBoxQuery(fieldName).setCornersOGC(bottomLeft, topRight), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case CONTAINS_ALL:
                        Object[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).toArray();
                        Stream.of(values).forEach(value -> addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, value), AND, not, nested, fieldFirstName));
                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                }
            }
        }
    }

    private Map<QueryType, Map<String, BoolQueryBuilder>> addFilterQuery(Map<QueryType, Map<String, BoolQueryBuilder>> nestedMap, BoolQueryBuilder boolQueryBuilder, final QueryBuilder queryBuilder, final LogicalOperator logicalOperator, final boolean not, final boolean nested, final String fieldFirstName) {
        QueryType queryType = getQueryType(logicalOperator, not);
        QueryBuilder query = queryBuilder;

        if (nested) {
            Optional<QueryBuilder> nestedQuery = addNestedQuery(queryType, nestedMap, fieldFirstName, queryBuilder, logicalOperator);
            if (nestedQuery.isPresent()) {
                query = nestedQuery.get();
            } else {
                return nestedMap;
            }
        }

        switch (queryType) {
            case FILTER:
                boolQueryBuilder.filter(query);
                break;

            case MUST_NOT:
                boolQueryBuilder.mustNot(query);
                break;

            case SHOULD:
                boolQueryBuilder.should(query);
                break;

            case SHOULD_NOT:
                boolQueryBuilder.should(boolQuery().mustNot(query));
                break;
        }
        return nestedMap;
    }

    enum QueryType {
        FILTER, MUST_NOT, SHOULD, SHOULD_NOT
    }

    private QueryType getQueryType(final LogicalOperator logicalOperator, final boolean not) {
        if(logicalOperator.equals(AND))
            return not ? MUST_NOT : FILTER;

        return not ? SHOULD_NOT : SHOULD;
    }

    private Optional<QueryBuilder> addNestedQuery(final QueryType queryType, Map<QueryType, Map<String, BoolQueryBuilder>> nestedMap, final String fieldFirstName, final QueryBuilder queryBuilder, final LogicalOperator logicalOperator) {
        final boolean nested = false;
        final boolean not = false;

        if (!nestedMap.containsKey(queryType)) {
            Map<String, BoolQueryBuilder> m = new HashMap<>();
            BoolQueryBuilder bq = boolQuery();
            m.put(fieldFirstName, bq);
            nestedMap.put(queryType, m);

            addFilterQuery(nestedMap, bq, queryBuilder, logicalOperator, not, nested, fieldFirstName);
            return of(nestedQuery(fieldFirstName, bq, None));

        } else if (!nestedMap.get(queryType).containsKey(fieldFirstName)) {
            BoolQueryBuilder bq = boolQuery();
            nestedMap.get(queryType).put(fieldFirstName, bq);

            addFilterQuery(nestedMap, bq, queryBuilder, logicalOperator, not, nested, fieldFirstName);
            return of(nestedQuery(fieldFirstName, bq, None));

        } else {
            addFilterQuery(nestedMap, nestedMap.get(queryType).get(fieldFirstName), queryBuilder, logicalOperator, not, nested, fieldFirstName);
        }

        return empty();
    }

    private boolean isNotBeforeCurrentQueryFragment(final QueryFragmentList queryFragmentList, final int index) {
        if (index - 1 >= 0) {
            QueryFragment before = queryFragmentList.get(index - 1);
            if (before instanceof QueryFragmentNot)
                return ((QueryFragmentNot) before).isNot();
        }
        return false;
    }
}
