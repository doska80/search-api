package com.vivareal.search.api.model.query;

import static java.util.stream.Collectors.joining;

import java.util.*;

public final class Sort extends AbstractSet<Item> {

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

  public Sort(
      Field field,
      OrderOperator orderOperator,
      Optional<GeoPointValue> geoPointValue,
      Optional<QueryFragment> queryFragment) {

    if (geoPointValue.isPresent()) {
      items.add(new GeoPointItem(field, geoPointValue.get(), queryFragment));
    } else {
      items.add(new Item(field, orderOperator, queryFragment));
    }
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
}
