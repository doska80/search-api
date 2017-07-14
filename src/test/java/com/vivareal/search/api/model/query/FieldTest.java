package com.vivareal.search.api.model.query;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class FieldTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldName() {
        new Field(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldName() {
        new Field(emptyList());
    }

    @Test
    public void testValidSimpleField() {
        Field field = new Field(asList("a"));
        assertEquals("a", field.getName());
        assertEquals("a", field.toString());
    }

    @Test
    public void testValidNestedField() {
        Field field = new Field(asList("a", "b"));
        assertEquals("a.b", field.getName());
        assertEquals("a.b", field.toString());
    }
}
