package com.vivareal.search.api.model.query;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValueTest {

    @Test
    public void nullValue() {
        Value value = new Value(null);
        assertEquals("NULL", value.toString());
        assertNull(value.getContents());
    }

    @Test
    public void singleObjectValue() {
        String valueRaw = "value";
        Value value = new Value(valueRaw);
        assertEquals(singletonList(valueRaw), value.getContents());
        assertEquals(valueRaw, value.getContents(0));
        assertEquals(String.format("\"%s\"", valueRaw), value.toString());
    }

    @Test
    public void multipleStringValue() {
        List<String> multiple = asList("\"value1\"", "\"value2\"", "\"value3\"");

        Value value = new Value(multiple);
        assertEquals(multiple, value.getContents());
        assertEquals("[\"value1\", \"value2\", \"value3\"]", value.toString());
    }
}
