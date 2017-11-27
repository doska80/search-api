package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.QueryFragment;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    private QueryParser queryParser;

    public QueryParserTest() {
        OperatorParser operatorParser = new OperatorParser();
        NotParser notParser = new NotParser();
        FilterParser filterParser = new FilterParser(new FieldParser(notParser), operatorParser, new ValueParser());
        this.queryParser = new QueryParser(operatorParser, filterParser, notParser);
    }

    @Test
    public void nonRecursiveMultipleConditionsTest() {
        QueryFragment query = queryParser.parse("field1 EQ 'value1' AND field2 NE 'value2' OR field3 GT 123 AND field4 NE 42");
        assertEquals("(field1 EQUAL \"value1\" AND field2 DIFFERENT \"value2\" OR field3 GREATER 123 AND field4 DIFFERENT 42)", query.toString());
    }

    @Test
    public void inTest() {
        QueryFragment query = queryParser.parse("banos IN [3,4]");
        assertEquals("(banos IN [3, 4])", query.toString());
    }

    @Test
    public void inMultipleTypesTest() {
        QueryFragment query = queryParser.parse("address.geoLocation IN [1, \"df\", true, null]");
        assertEquals("(address.geoLocation IN [1, \"df\", true, NULL])", query.toString());
    }

    @Test
    public void oneRecursionTest() {
        QueryFragment query = queryParser.parse("rooms:3 OR (parkingLots:1 AND xpto <> 3)");
        assertEquals("(rooms EQUAL 3 OR (parkingLots EQUAL 1 AND xpto DIFFERENT 3))", query.toString());
    }

    @Test
    public void oneRecursionAndMultipleConditionsTest() {
        QueryFragment query = queryParser.parse("rooms:3 AND pimba:2 AND(suites=1 OR (parkingLots IN [1,\"abc\"] AND xpto <> 3))");
        assertEquals("(rooms EQUAL 3 AND pimba EQUAL 2 AND (suites EQUAL 1 OR (parkingLots IN [1, \"abc\"] AND xpto DIFFERENT 3)))", query.toString());
    }

    @Test
    public void simpleAndBetweenFields() {
        QueryFragment query = queryParser.parse("a = 2 AND b = 3");
        assertEquals("(a EQUAL 2 AND b EQUAL 3)", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelSingle() {
        QueryFragment query = queryParser.parse("(a = 2) AND (b = 1)");
        assertEquals("((a EQUAL 2) AND (b EQUAL 1))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelMultiple() {
        QueryFragment query = queryParser.parse("((a = 2) AND (b = 3))");
        assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelWithAnd() {
        QueryFragment query = queryParser.parse("(a = 2) AND (b = 3)");
        assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void recursiveWithParenthesesOnTopLevelWithAndRecursive() {
        QueryFragment query = queryParser.parse("(a = 2 AND (b = 3))");
        assertEquals("(a EQUAL 2 AND (b EQUAL 3))", query.toString());
    }

    @Test
    public void testFilterAndNot() {
        QueryFragment query = queryParser.parse("suites=1 AND NOT a:\"a\"");
        assertEquals("(suites EQUAL 1 AND NOT a EQUAL \"a\")", query.toString());
    }

    @Test
    public void testFilterAndNotRecursive() {
        QueryFragment query = queryParser.parse("suites=1 AND (x=1 OR NOT a:\"a\")");
        assertEquals("(suites EQUAL 1 AND (x EQUAL 1 OR NOT a EQUAL \"a\"))", query.toString());
    }


    @Test
    public void testFilterAndNotRecursive2() {
        QueryFragment query = queryParser.parse("suites=1 AND (NOT a:\"a\")");
        assertEquals("(suites EQUAL 1 AND (NOT a EQUAL \"a\"))", query.toString());
    }

    @Test
    public void oneRecursionWithInsideNotTest() {
        QueryFragment query = queryParser.parse("(NOT suites=1)");
        assertEquals("(NOT suites EQUAL 1)", query.toString());
    }

    @Test(expected = ParserException.class)
    public void oneRecursionWithDoubleNotTest() {
        queryParser.parse("NOT NOT suites=1");
    }

    @Test
    public void totallyEquality() {
        QueryFragment query1 = queryParser.parse("rooms:3");
        QueryFragment query2 = queryParser.parse("(rooms:3)");
        assertEquals("(rooms EQUAL 3)", query1.toString());
        assertEquals("(rooms EQUAL 3)", query2.toString());
    }

    @Test
    public void enumName() {
        QueryFragment query = queryParser.parse("x LESS_EQUAL 10");
        assertEquals("(x LESS_EQUAL 10)", query.toString());
    }

    @Test(expected = ParserException.class)
    public void oneRecursionWithLogicalPrefix() {
        queryParser.parse("AND field:1");
    }

    @Test(expected = ParserException.class)
    public void exceededQueryFragmentLists() {
        String query = Collections
            .nCopies(QueryFragment.MAX_FRAGMENTS + 1, "field:1")
            .stream()
            .collect(Collectors.joining(" AND "));
        queryParser.parse(query);
    }
}
