package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FacetParserTest {

    @Test
    public void testOneFacetField() {
        List<Field> fields = FacetParser.parse("field");
        assertEquals("field", fields.get(0).toString());
    }

    @Test
    public void testMultipleFacetFields() {
        List<Field> fields = FacetParser.parse("field1, field2,       field3");
        assertEquals("field1", fields.get(0).toString());
        assertEquals("field2", fields.get(1).toString());
        assertEquals("field3", fields.get(2).toString());
    }

    @Test
    public void testMultipleFacetFieldsWithNested() {
        List<Field> fields = FacetParser.parse("field1, field1.field2,       field1.field2.field3");
        assertEquals("field1", fields.get(0).toString());
        assertEquals("field1.field2", fields.get(1).toString());
        assertEquals("field1.field2.field3", fields.get(2).toString());
    }

    @Test(expected = ParserException.class)
    public void testEmptyFacetField() {
        FacetParser.parse("");
    }

    @Test
    public void testMultipleFacetFieldsWithNot() {
        FacetParser.parse("field1, NOT field2");
    }
}
