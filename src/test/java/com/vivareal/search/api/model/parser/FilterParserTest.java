package com.vivareal.search.api.model.parser;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static org.junit.Assert.*;

import com.vivareal.search.api.model.query.Filter;
import com.vivareal.search.api.model.query.Value;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class FilterParserTest {

  private final FilterParser filterParser;

  public FilterParserTest() {
    this.filterParser =
        new FilterParser(fieldParserFixture(), new OperatorParser(), new ValueParser());
  }

  @Test
  public void testSingleExpressionWithDoubleQuotes() {
    filterParser.get().parse("field=\"value\"");
  }

  @Test
  public void testSingleExpressionWithNumberField() {
    Filter filter = filterParser.get().parse("field10=10");
    assertEquals("field10 EQUAL 10", filter.toString());
  }

  @Test
  public void testSingleExpressionWithSpacesAndSingleQuotes() {
    Filter filter = filterParser.get().parse("field.field2 = 'space value'");
    assertEquals("field.field2 EQUAL \"space value\"", filter.toString());
  }

  @Test
  public void testSingleExpressionWithINAndSpaces() {
    Filter filter = filterParser.get().parse("list IN [\"a\", 'b']");
    assertEquals("list IN [\"a\", \"b\"]", filter.toString());
  }

  @Test
  public void testEqualsLikeAsIN() {
    Filter filter = filterParser.get().parse("list = [\"a\", 'b']");
    assertEquals("list EQUAL [\"a\", \"b\"]", filter.toString());
  }

  @Test(expected = ParserException.class)
  public void testINLikeAsEquals() {
    filterParser.get().parse("list IN \"a\", \"b\"");
  }

  @Test
  public void testFilterEmpty() {
    Filter filter = filterParser.get().parse("field = \"\"");
    assertEquals("field EQUAL \"\"", filter.toString());
    assertFalse(filter.getValue().equals(Value.NULL_VALUE));
  }

  @Test
  public void testFilterNull() {
    Filter filter = filterParser.get().parse("field = NULL");
    assertEquals("field EQUAL NULL", filter.toString());
    assertTrue(filter.getValue().equals(Value.NULL_VALUE));
  }

  @Test
  public void testFilterQuotedNull() {
    Filter filter = filterParser.get().parse("field = 'NULL'");
    assertFalse(filter.getValue().equals(Value.NULL_VALUE));
  }

  @Test
  public void testFilterBooleanTrue() {
    Filter filterTrue = filterParser.get().parse("field = TRUE");
    Filter filterTrueLowerCase = filterParser.get().parse("field = true");
    assertEquals("field EQUAL true", filterTrue.toString());
    assertEquals(filterTrue.toString(), filterTrueLowerCase.toString());
  }

  @Test
  public void testFilterBooleanFalse() {
    Filter filterFalse = filterParser.get().parse("field = FALSE");
    Filter filterTrueLowerCase = filterParser.get().parse("field = false");
    assertEquals("field EQUAL false", filterFalse.toString());
    assertEquals(filterFalse.toString(), filterTrueLowerCase.toString());
  }

  @Test(expected = ParserException.class)
  public void testInvalidRelationalViewports() {
    filterParser
        .get()
        .parse("address.geoLocation EQ [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidSingleViewports() {
    filterParser.get().parse("address.geoLocation VIEWPORT [[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidMultipleViewports() {
    filterParser
        .get()
        .parse("address.geoLocation VIEWPORT [[-46.6597479],[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidMultipleViewportsOnSecond() {
    filterParser
        .get()
        .parse("address.geoLocation VIEWPORT [[-23.5534103,-46.6597479],[-23.5534103]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidViewportSingleValue() {
    filterParser.get().parse("address.geoLocation VIEWPORT \"df\"");
  }

  @Test(expected = ParserException.class)
  public void testEmptyViewportsWithoutValue() {
    filterParser.get().parse("address.geoLocation VIEWPORT");
  }

  @Test(expected = ParserException.class)
  public void testEmptyViewportsEmptyValue() {
    filterParser.get().parse("address.geoLocation VIEWPORT []");
  }

  @Test(expected = ParserException.class)
  public void testEmptyViewportsEmptyPoints() {
    filterParser.get().parse("address.geoLocation VIEWPORT [,]");
  }

  @Test
  public void testMultipleViewports() {
    String value =
        "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
    Filter viewport = filterParser.get().parse(value);
    assertEquals(
        "address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]",
        viewport.toString());
  }

  @Test(expected = ParserException.class)
  public void testSingleViewports() {
    String value = "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479]]";
    Filter viewport = filterParser.get().parse(value);
    assertEquals("address.geoLocation VIEWPORT [-23.5534103, -46.6597479]", viewport.toString());
  }

  @Test
  public void testMultipleViewportsWithAlias() {
    String value = "address.geoLocation @ [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
    Filter viewport = filterParser.get().parse(value);
    assertEquals(
        "address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]",
        viewport.toString());
  }

  @Test
  public void testSingleLike() {
    String value = "field LIKE '% \\% _ \\_ * \\n ? \\x'";
    Filter like = filterParser.get().parse(value);
    assertEquals("field LIKE \"* % ? _ \\* \n \\? \\x\"", like.toString());
  }

  @Test
  public void testSingleRange() {
    String value = "field RANGE [\"a\", 5]";
    Filter like = filterParser.get().parse(value);
    assertEquals("field RANGE [\"a\", 5]", like.toString());
  }

  @Test(expected = ParserException.class)
  public void testSingleInvalidRange() {
    String value = "field RANGE [1,]";
    filterParser.get().parse(value);
  }
}
