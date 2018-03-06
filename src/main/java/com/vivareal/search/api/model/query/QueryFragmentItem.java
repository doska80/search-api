package com.vivareal.search.api.model.query;

import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;

import com.google.common.base.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public class QueryFragmentItem implements QueryFragment {

  private LogicalOperator logicalOperator;
  private Filter filter;

  private QueryFragmentItem() {}

  public QueryFragmentItem(Optional<LogicalOperator> logicalOperator, Filter filter) {
    this.logicalOperator = logicalOperator.orElse(null);
    this.filter = filter;
  }

  public LogicalOperator getLogicalOperator() {
    return logicalOperator;
  }

  public Filter getFilter() {
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryFragmentItem item = (QueryFragmentItem) o;

    return Objects.equal(this.filter, item.filter)
        && Objects.equal(this.logicalOperator, item.logicalOperator);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.filter, this.logicalOperator);
  }

  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner(" ");
    ofNullable(logicalOperator).map(LogicalOperator::toString).ifPresent(sj::add);
    sj.add(filter.toString());
    return sj.toString();
  }

  @Override
  public Set<String> getFieldNames(boolean includeRootFields) {
    return includeRootFields
        ? filter.getField().getNames()
        : singleton(filter.getField().getName());
  }
}
