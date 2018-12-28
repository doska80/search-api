package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.SINGLE;
import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.VIEWPORT;
import static org.junit.Assert.assertEquals;

import com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.grupozap.search.api.model.query.Value;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class ValueParserTest {

  private final ValueParser valueParser = new ValueParser();

  @Test
  public void testInteger() {
    String value = "123456";
    Value parsed = valueParser.get().parse(value);
    assertEquals(Long.valueOf(value), parsed.first());
  }

  @Test
  public void testFloat() {
    String value = "123.456";
    Value parsed = valueParser.get().parse(value);
    assertEquals(Double.valueOf(value), parsed.first());
  }

  @Test(expected = ParserException.class)
  public void testUnquotedString() {
    valueParser.get().parse("unquoted");
  }

  @Test(expected = ParserException.class)
  public void testSpacedUnquotedString() {
    valueParser.get().parse("broken unquoted");
  }

  @Test
  public void testSingleQuotedString() {
    String value = "'single-quoted and with a lot of spaces'";
    Value parsed = valueParser.get().parse(value);
    assertEquals(value.substring(1, value.length() - 1), parsed.first());
  }

  @Test
  public void testDoubleQuotedString() {
    String value = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
    Value parsed = valueParser.get().parse(value);
    assertEquals(value.substring(1, value.length() - 1), parsed.first());
  }

  @Test
  public void testUsingIN() {
    String value = "[1.2,'2',3,\"4\"]";
    Value parsed = valueParser.get().parse(value);
    assertEquals("[1.2, \"2\", 3, \"4\"]", parsed.toString());
  }

  @Test
  public void testUsingINWithSpaces() {
    String in = "[   1.2 ,'2   ',          3    ,   \"   4   \"     ]";
    Value parsed = valueParser.get().parse(in);
    assertEquals("[1.2, \"2\", 3, \"4\"]", parsed.toString());
  }

  @Test(expected = ParserException.class)
  public void testUnquotedUsingIN() {
    valueParser.get().parse("[   1.2 ,'2   ',          3    ,   \"   4   \"  , error   ]");
  }

  @Test
  public void testNullValue() {
    Value nullValue = valueParser.get().parse("NULL");
    Value nullValueLowerCase = valueParser.get().parse("null");
    assertEquals(nullValue, Value.NULL_VALUE);
    assertEquals(nullValueLowerCase, Value.NULL_VALUE);
    assertEquals(nullValue, nullValueLowerCase);
  }

  @Test
  public void testBooleanFalseValue() {
    Value actual = new Value(false);

    Value falseValue = valueParser.get().parse("FALSE");
    assertEquals(falseValue, actual);
    assertEquals(falseValue, new Value(Boolean.FALSE));

    Value falseValueLowerCase = valueParser.get().parse("false");
    assertEquals(falseValueLowerCase, actual);
    assertEquals(falseValue, falseValueLowerCase);
  }

  @Test
  public void testBooleanTrueValue() {
    Value actual = new Value(true);

    Value trueValue = valueParser.get().parse("TRUE");
    assertEquals(trueValue, actual);
    assertEquals(trueValue, new Value(Boolean.TRUE));

    Value trueValueLowerCase = valueParser.get().parse("true");
    assertEquals(trueValueLowerCase, actual);
    assertEquals(trueValue, trueValueLowerCase);
  }

  @Test
  public void testSimpleNegativeDoubleIN() {
    String value = "[-23.5534103,-46.6597479]";
    Value viewport = valueParser.get().parse(value);
    assertEquals("[-23.5534103, -46.6597479]", viewport.toString());
  }

  @Test
  public void testMultipleNegativeDoubleIN() {
    String value = "[[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
    Value viewport = valueParser.getGeoPointValue(VIEWPORT).parse(value);
    assertEquals("[[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]", viewport.toString());
  }

  @Test
  public void testNegativeDoubleValuesLikeIN() {
    String value = "[-23.5534103  ,   -46.6597479, -23.5534103,-46.6597479  ]";
    Value in = valueParser.get().parse(value);
    assertEquals("[-23.5534103, -46.6597479, -23.5534103, -46.6597479]", in.toString());
  }

  @Test(expected = ParserException.class)
  public void testInvalidFirstMultipleNegativeDoubleIN() {
    valueParser.getGeoPointValue(VIEWPORT).parse("[[-23.5534103,-46.6597479],[-23.5534103]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidSecondMultipleNegativeDoubleIN() {
    valueParser.getGeoPointValue(VIEWPORT).parse("[[-46.6597479],[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidMultipleNegativeDoubleIN() {
    valueParser
        .getGeoPointValue(VIEWPORT)
        .parse("[[-23.5534103,-46.6597479],[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTypeGeoPoint() {
    valueParser.getGeoPointValue(Type.valueOf("invalidtype")).parse("[[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidSyntaxSingleGeoPoint() {
    valueParser.getGeoPointValue(SINGLE).parse("[[-23.5534103,-46.6597479]]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidSingleNegativeSingleGeoPoint() {
    valueParser.getGeoPointValue(SINGLE).parse("[-23.5534103]");
  }

  @Test(expected = ParserException.class)
  public void testInvalidMultipleNegativeSingleGeoPoint() {
    valueParser.getGeoPointValue(SINGLE).parse("[-23.5534103,-46.6597479, -40.1234567]");
  }
}
