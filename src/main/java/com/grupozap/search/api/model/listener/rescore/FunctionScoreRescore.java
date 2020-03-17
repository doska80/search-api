package com.grupozap.search.api.model.listener.rescore;

import static org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.SUM;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.scriptFunction;
import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;
import static org.elasticsearch.script.ScriptType.INLINE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.script.Script;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class FunctionScoreRescore extends SortRescore {

  private int weight;

  private ScriptScore script;

  public ScriptScore getScript() {
    return script;
  }

  public int getWeight() {
    return weight;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return new FunctionScoreQueryBuilder(
            scriptFunction(
                    new Script(
                        INLINE,
                        DEFAULT_SCRIPT_LANG,
                        this.script.getSource(),
                        this.script.getParams()))
                .setWeight(this.getWeight()))
        .scoreMode(SUM);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(SnakeCaseStrategy.class)
  public static class ScriptScore {

    private String source;
    private Map<String, Object> params;

    public String getSource() {
      return source;
    }

    public Map<String, Object> getParams() {
      return params;
    }
  }
}
