package com.grupozap.search.api.query;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.collections4.MapUtils.isEmpty;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

public class LtrQueryBuilder extends AbstractQueryBuilder<LtrQueryBuilder> {

  private static final String NAME = "sltr";
  private static final ParseField MODEL_NAME_FIELD = new ParseField("model");
  private static final ParseField PARAMS_FIELD = new ParseField("params");
  private static final ParseField ACTIVE_FEATURES_FIELD = new ParseField("active_features");

  private String model;
  private Map<String, Object> params;
  private List<String> activeFeatures;

  private LtrQueryBuilder(String model) {
    this.model = model;
  }

  @Override
  protected void doWriteTo(StreamOutput out) throws IOException {
    out.writeOptionalString(model);
    out.writeMap(params);
    out.writeOptionalStringArray(
        activeFeatures != null ? activeFeatures.toArray(new String[0]) : null);
  }

  @Override
  protected void doXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject(NAME);
    builder.field(MODEL_NAME_FIELD.getPreferredName(), model);
    builder.field(
        PARAMS_FIELD.getPreferredName(), isEmpty(this.params) ? newHashMap() : this.params);
    if (this.activeFeatures != null && !this.activeFeatures.isEmpty()) {
      builder.field(ACTIVE_FEATURES_FIELD.getPreferredName(), this.activeFeatures);
    }
    printBoostAndQueryName(builder);
    builder.endObject();
  }

  @Override
  protected Query doToQuery(QueryShardContext context) {
    return new SltrQuery(model, params, activeFeatures);
  }

  @Override
  protected boolean doEquals(LtrQueryBuilder other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    if (!super.equals(other)) return false;
    if (!Objects.equals(model, other.model)) return false;
    if (!Objects.equals(params, other.params)) return false;
    return Objects.equals(activeFeatures, other.activeFeatures);
  }

  @Override
  protected int doHashCode() {
    int result = super.hashCode();
    result = 31 * result + (model != null ? model.hashCode() : 0);
    result = 31 * result + (params != null ? params.hashCode() : 0);
    result = 31 * result + (activeFeatures != null ? activeFeatures.hashCode() : 0);
    return result;
  }

  @Override
  public String getWriteableName() {
    return NAME;
  }

  private static class SltrQuery extends Query {

    private final String model;
    private final Map<String, Object> params;
    private final List<String> activeFeatures;

    SltrQuery(String model, Map<String, Object> params, List<String> activeFeatures) {
      this.model = model;
      this.params = params;
      this.activeFeatures = activeFeatures;
    }

    @Override
    public String toString(String field) {
      return new ToStringBuilder(this)
          .append("SltrQuery(")
          .append("model", model)
          .append("params", params)
          .append("activeFeatures", activeFeatures)
          .append(")")
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SltrQuery sltrQuery = (SltrQuery) o;

      if (!Objects.equals(model, sltrQuery.model)) return false;
      if (!Objects.equals(params, sltrQuery.params)) return false;
      return Objects.equals(activeFeatures, sltrQuery.activeFeatures);
    }

    @Override
    public int hashCode() {
      int result = model != null ? model.hashCode() : 0;
      result = 31 * result + (params != null ? params.hashCode() : 0);
      result = 31 * result + (activeFeatures != null ? activeFeatures.hashCode() : 0);
      return result;
    }
  }

  public static class Builder {

    private final String model;
    private Map<String, Object> params;
    private List<String> activeFeatures;

    public Builder(String model) {
      this.model = model;
    }

    public Builder params(Map<String, Object> params) {
      this.params = params;
      return this;
    }

    public Builder activeFeatures(List<String> activeFeatures) {
      this.activeFeatures = activeFeatures;
      return this;
    }

    public LtrQueryBuilder build() {
      var ltrQueryBuilder = new LtrQueryBuilder(model);
      ltrQueryBuilder.params = this.params;
      ltrQueryBuilder.activeFeatures = this.activeFeatures;
      return ltrQueryBuilder;
    }
  }
}
