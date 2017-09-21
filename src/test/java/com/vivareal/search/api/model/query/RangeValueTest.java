package com.vivareal.search.api.model.query;

import org.junit.Test;

import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.assertEquals;

public class RangeValueTest {

    @Test
    public void testRangeValueConstructorByInt() {
        Value from = new Value(1);
        Value to = new Value(5);

        RangeValue rangeValue = new RangeValue(new Value(newArrayList(from, to)));
        assertEquals(from.value(), (Integer) ((Value) rangeValue.getContents(0)).value());
        assertEquals(to.value(), (Integer) ((Value) rangeValue.getContents(1)).value());
        assertEquals("[1, 5]", rangeValue.toString());
    }

    @Test
    public void testRangeValueConstructorByString() {
        Value from = new Value("a");
        Value to = new Value("c");

        RangeValue rangeValue = new RangeValue(new Value(newArrayList(from, to)));
        assertEquals(from.value(), (String) ((Value) rangeValue.getContents(0)).value());
        assertEquals(to.value(), (String) ((Value) rangeValue.getContents(1)).value());
        assertEquals("[\"a\", \"c\"]", rangeValue.toString());
    }

    @Test
    public void testRangeValueConstructorByDouble() {
        Value from = new Value(1.5);
        Value to = new Value(5.5);

        RangeValue rangeValue = new RangeValue(new Value(newArrayList(from, to)));
        assertEquals(from.value(), (Double) ((Value) rangeValue.getContents(0)).value());
        assertEquals(to.value(), (Double) ((Value) rangeValue.getContents(1)).value());
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
