package com.vivareal.search.api.model.query;

import org.junit.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class QueryFragmentNotTest {

    @Test
    public void validTrueNotValue() {
        QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(true));
        assertEquals("NOT", queryFragmentNot.toString());
    }

    @Test
    public void validFalseNotValue() {
        QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(false));
        assertEquals("", queryFragmentNot.toString());
    }

    @Test
    public void nullNotValue() {
        QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(null));
        assertEquals("", queryFragmentNot.toString());
    }

    @Test
    public void emptyNotValue() {
        QueryFragmentNot queryFragmentNot = new QueryFragmentNot(new ArrayList<>());
        assertEquals("", queryFragmentNot.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleNotValue() {
        QueryFragmentNot queryFragmentNot = new QueryFragmentNot(asList(true, true));
        assertEquals("", queryFragmentNot.toString());
    }
}
