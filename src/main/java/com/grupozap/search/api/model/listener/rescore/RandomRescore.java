package com.grupozap.search.api.model.listener.rescore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class RandomRescore extends SortRescore {

  private int seed;
  private String field = "_seq_no";

  public int getSeed() {
    return seed;
  }

  public String getField() {
    return field;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return new FunctionScoreQueryBuilder(
        new RandomScoreFunctionBuilder().seed(this.seed).setField(this.field));
  }
}
