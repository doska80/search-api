package com.vivareal.search.api.model.parser;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static org.junit.Assert.assertEquals;

import com.vivareal.search.api.model.query.Field;
import java.util.List;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class FacetParserTest {

  private FacetParser facetParser;

  public FacetParserTest() {
    this.facetParser = new FacetParser(fieldParserFixture());
  }

  @Test
  public void testOneFacetField() {
    List<Field> fields = facetParser.parse("field");
    assertEquals("field", fields.get(0).toString());
  }

  @Test
  public void testMultipleFacetFields() {
    List<Field> fields = facetParser.parse("field1, field2,       field3");
    assertEquals("field1", fields.get(0).toString());
    assertEquals("field2", fields.get(1).toString());
    assertEquals("field3", fields.get(2).toString());
  }

  @Test
  public void testMultipleFacetFieldsWithNested() {
    List<Field> fields = facetParser.parse("field1, field1.field2,       field1.field2.field3");
    assertEquals("field1", fields.get(0).toString());
    assertEquals("field1.field2", fields.get(1).toString());
    assertEquals("field1.field2.field3", fields.get(2).toString());
  }

  @Test(expected = ParserException.class)
  public void testEmptyFacetField() {
    facetParser.parse("");
  }

  @Test
  public void testMultipleFacetFieldsWithNot() {
    facetParser.parse("field1, NOT field2");
  }
}
