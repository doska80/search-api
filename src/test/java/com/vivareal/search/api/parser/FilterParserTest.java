package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilterParserTest {

    @Test
    public void testSingleExpression() {
        Parser<Filter> parser = FilterParser.getOne();
        parser.parse("pedrito=gringo");
    }

    @Test
    public void testSingleExpressionWithSpaces() {
        Parser<Filter> parser = FilterParser.getOne();
        Filter filter = parser.parse("pedrito = 'gringo mardito'");
        assertEquals("pedrito EQUAL gringo mardito", filter.toString());
    }

    @Test
    public void testSingleExpressionWithParentheses() {
        Parser<Filter> parser = FilterParser.getMulti();
        parser.parse("(pedrito=gringo)");
    }

    @Test
    public void testExpressionWithSpacesAndParentheses() {
        Parser<Filter> parser = FilterParser.getMulti();
        Filter filter = parser.parse("(pedrito = 'gringo mardito')");
        assertEquals("pedrito EQUAL gringo mardito", filter.toString());
    }

}