package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FacetParserTest {

    private static Parser<List<Field>> parser = FacetParser.get();

    @Test
    public void testOneFacetField() {
        List<Field> fields = parser.parse("field");
        assertEquals("field", fields.get(0).toString());
    }

    @Test
    public void testMultipleFacetFields() {
        List<Field> fields = parser.parse("field1, field2,       field3");
        assertEquals("field1", fields.get(0).toString());
        assertEquals("field2", fields.get(1).toString());
        assertEquals("field3", fields.get(2).toString());
    }

    @Test
    public void testMultipleFacetFieldsWithNested() {
        List<Field> fields = parser.parse("field1, field1.field2,       field1.field2.field3");
        assertEquals("field1", fields.get(0).toString());
        assertEquals("field1.field2", fields.get(1).toString());
        assertEquals("field1.field2.field3", fields.get(2).toString());
    }

    @Test(expected = ParserException.class)
    public void testEmptyFacetField() {
        parser.parse("");
    }

    @Ignore
    @Test(expected = ParserException.class)
    public void testMultipleFacetFieldsWithNot() {
        parser.parse("field1, NOT field2");
    }
}
