package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Queryable;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.QS_DEFAULT_FIELDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.QS_MM;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_STRING;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

@Component
public class QueryStringAdapter {

    private static final String NOT_NESTED = "not_nested";
    private static final String MM_ERROR_MESSAGE = "Minimum Should Match (mm) should be a valid integer number (-100 <> +100)";

    private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    public QueryStringAdapter(@Qualifier("elasticsearchSettings") SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter) {
        this.settingsAdapter = settingsAdapter;
    }

    private void checkMM(final String mm) {
        if (mm.contains(".") || mm.contains("%") && ((mm.length() - 1) > mm.indexOf('%')))
            throw new NumberFormatException(MM_ERROR_MESSAGE);

        String mmNumber = mm.replace("%", "");

        if (!isCreatable(mmNumber))
            throw new NumberFormatException(MM_ERROR_MESSAGE);

        int number = toInt(mmNumber);

        if (number < -100 || number > 100)
            throw new IllegalArgumentException(MM_ERROR_MESSAGE);
    }

    private MultiMatchQueryBuilder buildQueryStringQuery(MultiMatchQueryBuilder multiMatchQueryBuilder, final String indexName, final String q, final String[] boostFieldValues, final String mm) {
        if (multiMatchQueryBuilder == null)
            multiMatchQueryBuilder = multiMatchQuery(q);

        String fieldName = boostFieldValues[0];

        settingsAdapter.checkFieldName(indexName, fieldName, false);

        float boost = (boostFieldValues.length == 2 ? Float.parseFloat(boostFieldValues[1]) : 1.0f);
        multiMatchQueryBuilder.field(fieldName, boost)
            .minimumShouldMatch(mm)
            .tieBreaker(0.2f);
        return multiMatchQueryBuilder;
    }

    public void apply(BoolQueryBuilder queryBuilder, final Queryable request) {
        if (isEmpty(request.getQ()))
            return;

        String indexName = request.getIndex();

        String mm = isEmpty(request.getMm()) ? QS_MM.getValue(indexName) : request.getMm();
        checkMM(mm);

        Map<String, AbstractQueryBuilder> queryStringQueries = new HashMap<>();

        QS_DEFAULT_FIELDS.getValue(request.getFields(), indexName).forEach(field -> {
            String[] boostFieldValues = field.split(":");
            String fieldName = boostFieldValues[0];

            if (settingsAdapter.isTypeOf(indexName, fieldName.split("\\.")[0], FIELD_TYPE_NESTED)) {
                String nestedField = fieldName.split("\\.")[0];

                if (queryStringQueries.containsKey(nestedField))
                    buildQueryStringQuery((MultiMatchQueryBuilder) ((NestedQueryBuilder) queryStringQueries.get(nestedField)).query(), indexName, request.getQ(), boostFieldValues, mm);
                else
                    queryStringQueries.put(nestedField, nestedQuery(nestedField, buildQueryStringQuery(null, indexName, request.getQ(), boostFieldValues, mm), None));
            } else {
                if (queryStringQueries.containsKey(NOT_NESTED))
                    buildQueryStringQuery((MultiMatchQueryBuilder) queryStringQueries.get(NOT_NESTED), indexName, request.getQ(), boostFieldValues, mm);
                else
                    queryStringQueries.put(NOT_NESTED, buildQueryStringQuery(null, indexName, request.getQ(), boostFieldValues, mm));
            }
        });
        queryStringQueries.forEach((nestedPath, nestedQuery) -> queryBuilder.must().add(nestedQuery));
    }
}
