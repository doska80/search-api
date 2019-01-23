package com.grupozap.search.api.model.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RangeValueTest {

  @Test
  public void testRangeValueConstructorByInt() {
    var rangeValue = new RangeValue(1, 5);
    assertEquals("[1, 5]", rangeValue.toString());
  }

  @Test
  public void testRangeValueConstructorByString() {
    var rangeValue = new RangeValue("a", "c");
    assertEquals("[\"a\", \"c\"]", rangeValue.toString());
  }

  @Test
  public void testRangeValueConstructorByDouble() {
    var rangeValue = new RangeValue(1.5, 5.5);
    assertEquals("[1.5, 5.5]", rangeValue.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRangeValueConstructorUsingOnlyArgument() {
    new RangeValue(new Value(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRangeValueConstructorByNull() {
    new RangeValue(new Value(null));
  }
}
