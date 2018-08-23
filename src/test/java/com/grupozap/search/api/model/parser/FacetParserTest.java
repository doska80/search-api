package com.grupozap.search.api.model.parser;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.facetParserFixture;
import static org.junit.Assert.assertEquals;

import com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader;
import com.grupozap.search.api.model.query.Facet;
import com.vivareal.search.api.model.query.Facet;
import java.util.List;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class FacetParserTest {

  private final FacetParser facetParser;

  public FacetParserTest() {
    this.facetParser = ParserTemplateLoader.facetParserFixture();
  }

  @Test
  public void testOneFacetField() {
    List<Facet> fields = facetParser.parse("field");

    assertEquals("field _count DESC", fields.get(0).toString());
  }

  @Test
  public void testMultipleFacetFields() {
    List<Facet> fields = facetParser.parse("field1, field2,       field3");
    assertEquals("field1 _count DESC", fields.get(0).toString());
    assertEquals("field2 _count DESC", fields.get(1).toString());
    assertEquals("field3 _count DESC", fields.get(2).toString());
  }

  @Test
  public void testMultipleFacetFieldsWithNested() {
    List<Facet> fields = facetParser.parse("field1, field1.field2,       field1.field2.field3");
    assertEquals("field1 _count DESC", fields.get(0).toString());
    assertEquals("field1.field2 _count DESC", fields.get(1).toString());
    assertEquals("field1.field2.field3 _count DESC", fields.get(2).toString());
  }

  @Test
  public void testMultipleFacetSortFieldsWithNested() {
    List<Facet> fields =
        facetParser.parse(
            "field1, field2.field3,      field4 sortFacet  :    _key DESC, field5.field6 sortFacet:_count ASC, field7.field8 sortFacet: _key");

    assertEquals("field1 _count DESC", fields.get(0).toString());
    assertEquals("field2.field3 _count DESC", fields.get(1).toString());
    assertEquals("field4 _key DESC", fields.get(2).toString());
    assertEquals("field5.field6 _count ASC", fields.get(3).toString());
    assertEquals("field7.field8 _key ASC", fields.get(4).toString());
  }

  @Test
  public void testMultipleFacetFieldsWithNot() {
    facetParser.parse("field1, NOT field2");
  }

  @Test(expected = ParserException.class)
  public void testInvalidFacet() {
    facetParser.parse("field4 sortFacet:");
  }

  @Test(expected = ParserException.class)
  public void testInvalidFacetSortKey() {
    facetParser.parse("field4 xxx :  _key ASC");
  }

  @Test(expected = ParserException.class)
  public void testInvalidFacetFieldName() {
    facetParser.parse("field4 sortFacet :  blabla ASC");
  }
}
