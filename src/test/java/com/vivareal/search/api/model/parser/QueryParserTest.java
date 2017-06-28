package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.query.QueryFragmentList;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {
    private static Parser<QueryFragment> parser = QueryParser.get();

    @Test
    public void nonRecursiveMultipleConditionsTest() {
        QueryFragment query = parser.parse("field1 EQ 'value1' AND field2 NE 'value2' OR field3 GT 123 AND field4 NE 42");
        assertEquals("(field1 EQUAL \"value1\" AND field2 DIFFERENT \"value2\" OR field3 GREATER 123 AND field4 DIFFERENT 42)", query.toString());
    }

    @Test
    public void inTest() {
        QueryFragment query = parser.parse("banos IN [3,4]");
        assertEquals("(banos IN [3, 4])", query.toString());
    }

    @Test
    public void oneRecursionTest() {
        QueryFragment query = parser.parse("rooms:3 OR (parkingLots:1 AND xpto <> 3)");
        assertEquals("(rooms EQUAL 3 OR (parkingLots EQUAL 1 AND xpto DIFFERENT 3))", query.toString());
    }

    @Test
    public void oneRecursionAndMultipleConditionsTest() {
        QueryFragment query = parser.parse("rooms:3 AND pimba:2 AND(suites=1 OR (parkingLots IN [1,\"abc\"] AND xpto <> 3))");
        assertEquals("(rooms EQUAL 3 AND pimba EQUAL 2 AND (suites EQUAL 1 OR (parkingLots IN [1, \"abc\"] AND xpto DIFFERENT 3)))", query.toString());
    }

    @Test
    public void simpleAndBetweenFields() {
        QueryFragment query = parser.parse("a = 2 AND b = 3");
        assertEquals("(a EQUAL 2 AND b EQUAL 3)", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelSingle() {
        QueryFragment query = parser.parse("(a = 2) AND (b = 1)", Parser.Mode.DEBUG);
        assertEquals("((a EQUAL 2) AND (b EQUAL 1))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelMultiple() {
        QueryFragment query = parser.parse("((a = 2) AND (b = 3))");
        assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelWithAnd() {
        QueryFragment query = parser.parse("(a = 2) AND (b = 3)");
        assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelWithAndRecursive() {
        QueryFragment query = parser.parse("(a = 2 AND (b = 3))");
        assertEquals("(a EQUAL 2 AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void testFilterAndNot() {
        QueryFragment query = parser.parse("suites=1 AND NOT a:\"a\"");
        assertEquals("(suites EQUAL 1 AND NOT a EQUAL \"a\")", query.toString());
    }

    @Test
    public void testFilterAndNotRecursive() {
        QueryFragment query = parser.parse("suites=1 AND (x=1 OR NOT a:\"a\")");
        assertEquals("(suites EQUAL 1 AND (x EQUAL 1 OR NOT a EQUAL \"a\"))", query.toString());
    }


    @Test
    public void testFilterAndNotRecursive2() {
        QueryFragment query = parser.parse("suites=1 AND (NOT a:\"a\")");
        assertEquals("(suites EQUAL 1 AND (NOT a EQUAL \"a\"))", query.toString());
    }

    @Test
    public void oneRecursionWithInsideNotTest() {
        QueryFragment query = parser.parse("(NOT suites=1)");
        assertEquals("(NOT suites EQUAL 1)", query.toString());
    }

    @Test(expected = ParserException.class)
    public void oneRecursionWithDoubleNotTest() {
        parser.parse("NOT NOT suites=1");
    }

    @Test
    public void totallyEquality() {
        QueryFragment query1 = parser.parse("rooms:3");
        QueryFragment query2 = parser.parse("(rooms:3)");
        assertEquals("(rooms EQUAL 3)", query1.toString());
        assertEquals("(rooms EQUAL 3)", query2.toString());
    }
}
