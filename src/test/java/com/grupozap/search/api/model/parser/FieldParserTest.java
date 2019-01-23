package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldCacheFixture;
import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class FieldParserTest {

  private final FieldParser fieldParser;

  public FieldParserTest() {
    this.fieldParser = new FieldParser(new NotParser(), fieldCacheFixture());
  }

  @Test
  public void testValidFieldNames() {
    var fieldNames =
        new String[] {
          "field",
          "field10",
          "fieldCamelCase",
          "_fieldUnderscoredCamelCase",
          "field1CamelCase2With3Numbers"
        };
    var parser = fieldParser.get();
    Stream.of(fieldNames)
        .forEach(
            fieldName -> {
              var parsedField = parser.parse(fieldName);
              assertEquals(fieldName, parsedField.getName());
            });
  }

  @Test(expected = ParserException.class)
  public void testInvalidFieldNamesWithSpaces() {
    fieldParser.get().parse("field with space");
  }

  @Test(expected = ParserException.class)
  public void testBlankFieldNames() {
    fieldParser.get().parse("");
  }

  @Test(expected = ParserException.class)
  public void testDotFieldNames() {
    fieldParser.get().parse(".");
  }

  @Test(expected = ParserException.class)
  public void testRootNestedFieldNames() {
    fieldParser.get().parse(".abc.def");
  }

  @Test
  public void testNestedFieldNames() {
    var field = fieldParser.get().parse("field.field2.field3");
    assertEquals(field.getName(), "field.field2.field3");
  }

  @Test(expected = ParserException.class)
  public void testDoublePointFieldNames() {
    fieldParser.get().parse("field..field2");
  }

  @Test(expected = ParserException.class)
  public void testDotEndedFieldNames() {
    fieldParser.get().parse("field.");
  }

  @Test(expected = ParserException.class)
  public void testInvalidFieldNamesWithSpecialChars() {
    fieldParser.get().parse("ãçéntêdFïeld");
  }

  @Test
  public void testStringNames() {
    var field = fieldParser.get().parse("field.field2.field3.field4");
    assertEquals("field.field2.field3.field4", field.toString());
  }

  @Test(expected = ParserException.class)
  public void testFieldNotWithInvalidPointEnding() {
    fieldParser.get().parse("NOT field.");
  }

  @Test(expected = ParserException.class)
  public void testFieldNotWithInvalidPoint() {
    fieldParser.get().parse("NOT .");
  }

  @Test(expected = ParserException.class)
  public void testNotWithBlankFieldName() {
    fieldParser.get().parse("NOT ");
  }
}
