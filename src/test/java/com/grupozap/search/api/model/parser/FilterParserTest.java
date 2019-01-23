package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static org.junit.Assert.*;

import com.grupozap.search.api.model.query.Value;
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
    var filter = filterParser.get().parse("field10=10");
    assertEquals("field10 EQUAL 10", filter.toString());
  }

  @Test
  public void testSingleExpressionWithSpacesAndSingleQuotes() {
    var filter = filterParser.get().parse("field.field2 = 'space value'");
    assertEquals("field.field2 EQUAL \"space value\"", filter.toString());
  }

  @Test
  public void testSingleExpressionWithINAndSpaces() {
    var filter = filterParser.get().parse("list IN [\"a\", 'b']");
    assertEquals("list IN [\"a\", \"b\"]", filter.toString());
  }

  @Test
  public void testEqualsLikeAsIN() {
    var filter = filterParser.get().parse("list = [\"a\", 'b']");
    assertEquals("list EQUAL [\"a\", \"b\"]", filter.toString());
  }

  @Test(expected = ParserException.class)
  public void testINLikeAsEquals() {
    filterParser.get().parse("list IN \"a\", \"b\"");
  }

  @Test
  public void testFilterEmpty() {
    var filter = filterParser.get().parse("field = \"\"");
    assertEquals("field EQUAL \"\"", filter.toString());
    assertNotEquals(filter.getValue(), Value.NULL_VALUE);
  }

  @Test
  public void testFilterNull() {
    var filter = filterParser.get().parse("field = NULL");
    assertEquals("field EQUAL NULL", filter.toString());
    assertEquals(filter.getValue(), Value.NULL_VALUE);
  }

  @Test
  public void testFilterQuotedNull() {
    var filter = filterParser.get().parse("field = 'NULL'");
    assertNotEquals(filter.getValue(), Value.NULL_VALUE);
  }

  @Test
  public void testFilterBooleanTrue() {
    var filterTrue = filterParser.get().parse("field = TRUE");
    var filterTrueLowerCase = filterParser.get().parse("field = true");
    assertEquals("field EQUAL true", filterTrue.toString());
    assertEquals(filterTrue.toString(), filterTrueLowerCase.toString());
  }

  @Test
  public void testFilterBooleanFalse() {
    var filterFalse = filterParser.get().parse("field = FALSE");
    var filterTrueLowerCase = filterParser.get().parse("field = false");
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
    var value =
        "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
    var viewport = filterParser.get().parse(value);
    assertEquals(
        "address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]",
        viewport.toString());
  }

  @Test(expected = ParserException.class)
  public void testSingleViewports() {
    var value = "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479]]";
    var viewport = filterParser.get().parse(value);
    assertEquals("address.geoLocation VIEWPORT [-23.5534103, -46.6597479]", viewport.toString());
  }

  @Test
  public void testMultipleViewportsWithAlias() {
    var value = "address.geoLocation @ [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
    var viewport = filterParser.get().parse(value);
    assertEquals(
        "address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]",
        viewport.toString());
  }

  @Test
  public void testSingleLike() {
    var value = "field LIKE '% \\% _ \\_ * \\n ? \\x'";
    var like = filterParser.get().parse(value);
    assertEquals("field LIKE \"* % ? _ \\* \n \\? \\x\"", like.toString());
  }

  @Test
  public void testSingleRange() {
    var value = "field RANGE [\"a\", 5]";
    var like = filterParser.get().parse(value);
    assertEquals("field RANGE [\"a\", 5]", like.toString());
  }

  @Test(expected = ParserException.class)
  public void testSingleInvalidRange() {
    var value = "field RANGE [1,]";
    filterParser.get().parse(value);
  }
}
