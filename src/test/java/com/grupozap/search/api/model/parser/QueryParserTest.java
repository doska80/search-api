package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.filterParserFixture;
import static org.junit.Assert.assertEquals;

import com.grupozap.search.api.model.query.QueryFragment;
import java.util.Collections;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class QueryParserTest {

  private final QueryParser queryParser;

  public QueryParserTest() {
    this.queryParser =
        new QueryParser(new OperatorParser(), filterParserFixture(), new NotParser());
  }

  @Test
  public void nonRecursiveMultipleConditionsTest() {
    var query =
        queryParser.parse(
            "field1 EQ 'value1' AND field2 NE 'value2' OR field3 GT 123 AND field4 NE 42");
    assertEquals(
        "(field1 EQUAL \"value1\" AND field2 DIFFERENT \"value2\" OR field3 GREATER 123 AND field4 DIFFERENT 42)",
        query.toString());
  }

  @Test
  public void inTest() {
    var query = queryParser.parse("banos IN [3,4,2164325564]");
    assertEquals("(banos IN [3, 4, 2164325564])", query.toString());
  }

  @Test
  public void inMultipleTypesTest() {
    var query = queryParser.parse("address.geoLocation IN [1, \"df\", true, null]");
    assertEquals("(address.geoLocation IN [1, \"df\", true, NULL])", query.toString());
  }

  @Test
  public void oneRecursionTest() {
    var query = queryParser.parse("rooms:3 OR (parkingLots:1 AND xpto <> 3)");
    assertEquals("(rooms EQUAL 3 OR (parkingLots EQUAL 1 AND xpto DIFFERENT 3))", query.toString());
  }

  @Test
  public void oneRecursionAndMultipleConditionsTest() {
    var query =
        queryParser.parse(
            "rooms:3 AND pimba:2 AND(suites=1 OR (parkingLots IN [1,\"abc\"] AND xpto <> 3))");
    assertEquals(
        "(rooms EQUAL 3 AND pimba EQUAL 2 AND (suites EQUAL 1 OR (parkingLots IN [1, \"abc\"] AND xpto DIFFERENT 3)))",
        query.toString());
  }

  @Test
  public void simpleAndBetweenFields() {
    var query = queryParser.parse("a = 2 AND b = 3");
    assertEquals("(a EQUAL 2 AND b EQUAL 3)", query.toString());
  }

  @Test
  public void recursiveWithParenthesesOnTopLevelSingle() {
    var query = queryParser.parse("(a = 2) AND (b = 1)");
    assertEquals("((a EQUAL 2) AND (b EQUAL 1))", query.toString());
  }

  @Test
  public void recursiveWithParenthesesOnTopLevelMultiple() {
    var query = queryParser.parse("((a = 2) AND (b = 3))");
    assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
  }

  @Test
  public void recursiveWithParenthesesOnTopLevelWithAnd() {
    var query = queryParser.parse("(a = 2) AND (b = 3)");
    assertEquals("((a EQUAL 2) AND (b EQUAL 3))", query.toString());
  }

  @Test
  public void recursiveWithParenthesesOnTopLevelWithAndRecursive() {
    var query = queryParser.parse("(a = 2 AND (b = 3))");
    assertEquals("(a EQUAL 2 AND (b EQUAL 3))", query.toString());
  }

  @Test
  public void testFilterAndNot() {
    var query = queryParser.parse("suites=1 AND NOT a:\"a\"");
    assertEquals("(suites EQUAL 1 AND NOT a EQUAL \"a\")", query.toString());
  }

  @Test
  public void testFilterAndNotRecursive() {
    var query = queryParser.parse("suites=1 AND (x=1 OR NOT a:\"a\")");
    assertEquals("(suites EQUAL 1 AND (x EQUAL 1 OR NOT a EQUAL \"a\"))", query.toString());
  }

  @Test
  public void testFilterAndNotRecursive2() {
    var query = queryParser.parse("suites=1 AND (NOT a:\"a\")");
    assertEquals("(suites EQUAL 1 AND (NOT a EQUAL \"a\"))", query.toString());
  }

  @Test
  public void oneRecursionWithInsideNotTest() {
    var query = queryParser.parse("(NOT suites=1)");
    assertEquals("(NOT suites EQUAL 1)", query.toString());
  }

  @Test(expected = ParserException.class)
  public void oneRecursionWithDoubleNotTest() {
    queryParser.parse("NOT NOT suites=1");
  }

  @Test
  public void totallyEquality() {
    var query1 = queryParser.parse("rooms:3");
    var query2 = queryParser.parse("(rooms:3)");
    assertEquals("(rooms EQUAL 3)", query1.toString());
    assertEquals("(rooms EQUAL 3)", query2.toString());
  }

  @Test
  public void enumName() {
    var query = queryParser.parse("x LESS_EQUAL 10");
    assertEquals("(x LESS_EQUAL 10)", query.toString());
  }

  @Test(expected = ParserException.class)
  public void oneRecursionWithLogicalPrefix() {
    queryParser.parse("AND field:1");
  }

  @Test(expected = ParserException.class)
  public void exceededQueryFragmentLists() {
    var query =
        String.join(" AND ", Collections.nCopies(QueryFragment.MAX_FRAGMENTS + 1, "field:1"));
    queryParser.parse(query);
  }

  @Test
  public void testMultipleFieldsWithAndWithoutAlias() {
    var query =
        queryParser.parse(
            "field_before_alias EQ 'value1' AND field2 NE 'value2' OR field3 GT 123 AND field4 NE 42");
    assertEquals(
        "(field_after_alias EQUAL \"value1\" AND field2 DIFFERENT \"value2\" OR field3 GREATER 123 AND field4 DIFFERENT 42)",
        query.toString());
  }
}
