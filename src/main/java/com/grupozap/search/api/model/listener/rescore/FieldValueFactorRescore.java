package com.grupozap.search.api.model.listener.rescore;

import static org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.SUM;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class FieldValueFactorRescore extends SortRescore {

  private String field;
  private int missing;
  private int factor = 1;

  public String getField() {
    return field;
  }

  public int getMissing() {
    return missing;
  }

  public int getFactor() {
    return factor;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return new FunctionScoreQueryBuilder(
            new FieldValueFactorFunctionBuilder(field).missing(missing).factor(factor))
        .scoreMode(SUM);
  }
}
