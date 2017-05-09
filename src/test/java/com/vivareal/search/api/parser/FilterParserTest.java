package com.vivareal.search.api.parser;

import java.util.List;
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

    //FIXME @Test
    public void testSingleExpressionWithParentheses() {
        Parser<List<Expression>> parser = FilterParser.getMulti();
        parser.parse("(pedrito=gringo)");
    }

    //FIXME @Test
    public void testExpressionWithSpacesAndParentheses() {
        Parser<List<Expression>> parser = FilterParser.getMulti();
        List<Expression> expressions = parser.parse("(pedrito = 'gringo mardito')");
        assertEquals("pedrito EQUAL gringo mardito", expressions.get(0).<Filter>get());
    }

}