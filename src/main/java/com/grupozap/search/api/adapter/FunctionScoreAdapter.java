package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_FIELD;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_MAX_BOOST;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_MODIFIER;
import static java.lang.Double.valueOf;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.fromString;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.fieldValueFactorFunction;

import com.grupozap.search.api.model.parser.FieldParser;
import com.grupozap.search.api.model.search.Queryable;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

@Component
public class FunctionScoreAdapter {

  private final FieldParser fieldParser;

  public FunctionScoreAdapter(FieldParser fieldParser) {
    this.fieldParser = fieldParser;
  }

  public void apply(
      SearchSourceBuilder searchSourceBuilder, QueryBuilder queryBuilder, final Queryable request) {
    final var indexName = request.getIndex();
    String factorField =
        isEmpty(request.getFactorField())
            ? SCORE_FACTOR_FIELD.getValue(indexName)
            : request.getFactorField();
    if (isEmpty(factorField)) return;

    fieldParser.parse(factorField);

    var fieldValueFactorFunctionBuilder =
        fieldValueFactorFunction(factorField).missing(0);

    final String factorModifier =
        isEmpty(request.getFactorModifier())
            ? SCORE_FACTOR_MODIFIER.getValue(indexName)
            : request.getFactorModifier();
    if (isNotEmpty(factorModifier))
      fieldValueFactorFunctionBuilder.modifier(fromString(factorModifier));

    final var maxBoost =
        SCORE_FACTOR_MAX_BOOST.getValue(request.getIndex()) != null
            ? valueOf(SCORE_FACTOR_MAX_BOOST.getValue(request.getIndex()) + "").floatValue()
            : 1.0f;

    searchSourceBuilder.query(
        functionScoreQuery(queryBuilder, fieldValueFactorFunctionBuilder).maxBoost(maxBoost));
  }
}
