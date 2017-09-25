package com.vivareal.search.api.model.query;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.lang.Double.valueOf;
import static java.util.Arrays.asList;
import static org.apache.lucene.geo.GeoUtils.MIN_LAT_INCL;
import static org.junit.Assert.assertEquals;

public class ViewportValueTest {

    @Test(expected = IllegalArgumentException.class)
    public void emptyViewport() {
        new ViewportValue(Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void hugeViewport() {
        List<List<Value>> viewport = asList(
                asList(new Value(1), new Value(2)),
                asList(new Value(3), new Value(4)),
                asList(new Value(5), new Value(6)));

        new ViewportValue(viewport);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullViewport() {
        List<List<Value>> viewport = asList(asList(new Value(1), new Value(2)), null);
        new ViewportValue(viewport);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hugeItemViewport() {
        List<List<Value>> viewport = asList(
            asList(new Value(1), new Value(2)),
            asList(new Value(3), new Value(4), new Value(5)));

        new ViewportValue(viewport);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidViewportValues() {
        List<List<Value>> viewport = asList(
        asList(new Value(MIN_LAT_INCL - 100), new Value(2.0)),
        asList(new Value(3.3), new Value(4.0)));

        new ViewportValue(viewport);
    }

    @Test
    public void validViewport() {
        List<List<Value>> viewport = asList(
        asList(new Value(10.1), new Value(2.0)),
        asList(new Value(3.3), new Value(4.0)));

        ViewportValue viewportValue = new ViewportValue(viewport);

        assertEquals(valueOf(10.1), viewportValue.value(0, 0));
        assertEquals(valueOf(2.0), viewportValue.value(0, 1));
        assertEquals(valueOf(3.3), viewportValue.value(1, 0));
        assertEquals(valueOf(4.0), viewportValue.value(1, 1));
    }
}
