package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_FIELD;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_MODIFIER;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.fromString;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.fieldValueFactorFunction;

import com.vivareal.search.api.model.parser.FieldParser;
import com.vivareal.search.api.model.search.Queryable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.springframework.stereotype.Component;

@Component
public class FunctionScoreAdapter {

  private final FieldParser fieldParser;

  public FunctionScoreAdapter(FieldParser fieldParser) {
    this.fieldParser = fieldParser;
  }

  public void apply(
      SearchRequestBuilder searchRequestBuilder,
      QueryBuilder queryBuilder,
      final Queryable request) {
    final String indexName = request.getIndex();
    String factorField =
        isEmpty(request.getFactorField())
            ? SCORE_FACTOR_FIELD.getValue(indexName)
            : request.getFactorField();
    if (isEmpty(factorField)) return;

    fieldParser.parse(factorField);

    FieldValueFactorFunctionBuilder fieldValueFactorFunctionBuilder =
        fieldValueFactorFunction(factorField).missing(0);

    final String factorModifier =
        isEmpty(request.getFactorModifier())
            ? SCORE_FACTOR_MODIFIER.getValue(indexName)
            : request.getFactorModifier();
    if (isNotEmpty(factorModifier))
      fieldValueFactorFunctionBuilder.modifier(fromString(factorModifier));

    searchRequestBuilder.setQuery(
        functionScoreQuery(queryBuilder, fieldValueFactorFunctionBuilder));
  }
}
