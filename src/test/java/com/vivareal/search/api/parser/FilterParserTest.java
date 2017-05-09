package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilterParserTest {

    @Test
    public void testSingleExpression() {
        Parser<Filter> parser = FilterParser.get();
        parser.parse("pedrito=gringo");
    }

    @Test
    public void testSingleExpressionWithSpaces() {
        Parser<Filter> parser = FilterParser.get();
        Filter filter = parser.parse("pedrito = 'gringo mardito'");
        assertEquals("pedrito EQUAL gringo mardito", filter.toString());
    }

}