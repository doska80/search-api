package com.vivareal.search.api.model.query;

public class QueryFragmentOperator implements QueryFragment {

  private final LogicalOperator operator;

  public QueryFragmentOperator(LogicalOperator operator) {
    this.operator = operator;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryFragmentOperator that = (QueryFragmentOperator) o;

    return operator == that.operator;
  }

  @Override
  public int hashCode() {
    return operator != null ? operator.hashCode() : 0;
  }

  @Override
  public String toString() {
    return operator.name();
  }
}
