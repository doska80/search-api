package com.grupozap.search.api.model.query;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;

import com.google.common.base.Objects;
import java.util.Optional;

public class Item {
  private Field field;
  private OrderOperator orderOperator;
  private Optional<QueryFragment> queryFragment;

  public Item() {}

  public Item(Field field, OrderOperator orderOperator, Optional<QueryFragment> queryFragment) {
    this.field = field;
    this.orderOperator = orderOperator;
    this.queryFragment = queryFragment;
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public OrderOperator getOrderOperator() {
    return orderOperator;
  }

  public void setOrderOperator(OrderOperator orderOperator) {
    this.orderOperator = orderOperator;
  }

  public Optional<QueryFragment> getQueryFragment() {
    return queryFragment;
  }

  public void setQueryFragment(Optional<QueryFragment> queryFragment) {
    this.queryFragment = queryFragment;
  }

  @Override
  public String toString() {
    if (queryFragment != null) {
      return format(
              "%s %s %s",
              field, orderOperator, queryFragment.map(QueryFragment::toString).orElse(EMPTY))
          .trim();
    } else {
      return format("%s %s", field, orderOperator).trim();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    var item = (Item) o;
    return equal(field, item.field)
        && equal(orderOperator, item.orderOperator)
        && equal(queryFragment, item.queryFragment);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(field, orderOperator, queryFragment);
  }
}
