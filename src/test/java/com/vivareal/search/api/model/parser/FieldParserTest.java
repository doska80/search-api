package com.vivareal.search.api.model.parser;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldFactoryFixture;
import static org.junit.Assert.assertEquals;

import com.vivareal.search.api.model.query.Field;
import java.util.stream.Stream;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class FieldParserTest {

  private FieldParser fieldParser;

  public FieldParserTest() {
    this.fieldParser = new FieldParser(new NotParser(), fieldFactoryFixture());
  }

  @Test
  public void testValidFieldNames() {
    String[] fieldNames =
        new String[] {
          "field",
          "field10",
          "fieldCamelCase",
          "_fieldUnderscoredCamelCase",
          "field1CamelCase2With3Numbers"
        };
    Parser<Field> parser = fieldParser.get();
    Stream.of(fieldNames)
        .forEach(
            fieldName -> {
              Field parsedField = parser.parse(fieldName);
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
    Field field = fieldParser.get().parse("field.field2.field3");
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
    Field field = fieldParser.get().parse("field.field2.field3.field4");
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
