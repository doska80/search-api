package com.vivareal.search.api.model.query;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.EMPTY;

import com.google.common.base.Objects;
import java.util.*;

public final class Sort extends AbstractSet<Sort.Item> {

  private final Set<Item> items = new LinkedHashSet<>();

  public Sort(List<Sort> sortList) {
    sortList.stream().flatMap(s -> s.items.stream()).forEach(items::add);
  }

  public Sort(Field field, OrderOperator orderOperator, Optional<QueryFragment> queryFragment) {
    items.add(new Item(field, orderOperator, queryFragment));
  }

  public Sort(Field field, OrderOperator orderOperator) {
    this(field, orderOperator, Optional.empty());
  }

  @Override
  public Iterator<Item> iterator() {
    return items.iterator();
  }

  public Item getFirst() {
    if (items.isEmpty()) {
      throw new IllegalArgumentException("The sort is invalid: " + items.toString());
    }

    return items.iterator().next();
  }

  @Override
  public int size() {
    return items.size();
  }

  @Override
  public String toString() {
    return items.stream().map(Item::toString).collect(joining(" "));
  }

  public static class Item {
    private final Field field;
    private final OrderOperator orderOperator;
    private final Optional<QueryFragment> queryFragment;

    private Item(Field field, OrderOperator orderOperator, Optional<QueryFragment> queryFragment) {
      this.field = field;
      this.orderOperator = orderOperator;
      this.queryFragment = queryFragment;
    }

    private Item(Field field, OrderOperator orderOperator) {
      this.field = field;
      this.orderOperator = orderOperator;
      this.queryFragment = Optional.empty();
    }

    public Field getField() {
      return field;
    }

    public OrderOperator getOrderOperator() {
      return orderOperator;
    }

    public Optional<QueryFragment> getQueryFragment() {
      return queryFragment;
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
      Item item = (Item) o;
      return equal(field, item.field)
          && equal(orderOperator, item.orderOperator)
          && equal(queryFragment, item.queryFragment);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(field, orderOperator, queryFragment);
    }
  }
}
