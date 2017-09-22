package com.vivareal.search.api.model.query;

import org.junit.Test;

import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.assertEquals;

public class RangeValueTest {

    @Test
    public void testRangeValueConstructorByInt() {
        RangeValue rangeValue = new RangeValue(1, 5);
        assertEquals("[1, 5]", rangeValue.toString());
    }

    @Test
    public void testRangeValueConstructorByString() {
        RangeValue rangeValue = new RangeValue("a", "c");
        assertEquals("[\"a\", \"c\"]", rangeValue.toString());
    }

    @Test
    public void testRangeValueConstructorByDouble() {
        RangeValue rangeValue = new RangeValue(1.5, 5.5);
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
