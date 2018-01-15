package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Queryable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_FIELD;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_MODIFIER;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;

@Component
public class FunctionScoreAdapter {

    private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    public FunctionScoreAdapter(@Qualifier("elasticsearchSettings") SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter) {
        this.settingsAdapter = settingsAdapter;
    }

    public void apply(SearchRequestBuilder searchRequestBuilder, QueryBuilder queryBuilder, final Queryable request) {
        final String indexName = request.getIndex();
        final String factorField = isEmpty(request.getFactorField()) ? SCORE_FACTOR_FIELD.getValue(indexName) : request.getFactorField();
        if (isEmpty(factorField))
            return;

        settingsAdapter.checkFieldName(indexName, factorField, false);

        FieldValueFactorFunctionBuilder fieldValueFactorFunctionBuilder = ScoreFunctionBuilders.fieldValueFactorFunction(factorField).missing(0);

        final String factorModifier = isEmpty(request.getFactorModifier()) ? SCORE_FACTOR_MODIFIER.getValue(indexName) : request.getFactorModifier();
        if(isNotEmpty(factorModifier))
            fieldValueFactorFunctionBuilder.modifier(Modifier.fromString(factorModifier));

        searchRequestBuilder.setQuery(functionScoreQuery(queryBuilder, fieldValueFactorFunctionBuilder));
    }
}
