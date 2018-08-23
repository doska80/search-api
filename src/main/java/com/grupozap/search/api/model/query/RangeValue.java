package com.grupozap.search.api.model.query;

import static com.google.common.collect.Lists.newArrayList;

public class RangeValue extends Value {

  public RangeValue(Value value) {
    if (value.size() != 2)
      throw new IllegalArgumentException("The RANGE filter does not have from/to pair");

    this.contents = value.contents;
  }

  public RangeValue(Object from, Object to) {
    this(new Value(newArrayList(new Value(from), new Value(to))));
  }
}
