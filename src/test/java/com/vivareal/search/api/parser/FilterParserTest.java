package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterParserTest {

    @Test
    public void testSingleExpressionWithDoubleQuotes() {
        Parser<Filter> parser = FilterParser.get();
        parser.parse("field=\"value\"");
    }

    @Test
    public void testSingleExpressionWithSpacesAndSingleQuotes() {
        Parser<Filter> parser = FilterParser.get();
        Filter filter = parser.parse("field.field2 = 'space value'");
        assertEquals("field.field2 EQUAL space value", filter.toString());
    }

    @Test
    public void testSingleExpressionWithINAndSpaces() {
        Parser<Filter> parser = FilterParser.get();
        Filter filter = parser.parse("list IN [\"a\", 'b']");
        assertEquals("list IN [\"a\", \"b\"]", filter.toString());
    }

    @Test
    public void testEqualsLikeAsIN() {
        Filter filter = FilterParser.get().parse("list = [\"a\", 'b']");
        assertEquals("list EQUAL [\"a\", \"b\"]", filter.toString());
    }

    @Test(expected = ParserException.class)
    public void testINLikeAsEquals() {
        FilterParser.get().parse("list IN \"a\", \"b\"");
    }

    @Test
    public void testFilterEmpty() {
        Filter filter = FilterParser.get().parse("field = \"\"");
        assertEquals("field EQUAL \"\"", filter.toString());
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterNull() {
        Filter filter = FilterParser.get().parse("sobrinho = NULL");
        assertEquals("sobrinho EQUAL NULL", filter.toString());
        assertTrue(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterQuotedNull() {
        Filter filter = FilterParser.get().parse("field = 'NULL'");
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterNot() {
        Filter filter = FilterParser.get().parse("field = \"value\"");
        assertEquals("field EQUAL value", filter.toString());
        assertFalse(filter.isNot());

        Filter filterNot = FilterParser.get().parse("NOT field = \"value\"");
        assertEquals("NOT field EQUAL value", filterNot.toString());
        assertTrue(filterNot.isNot());
    }
}
