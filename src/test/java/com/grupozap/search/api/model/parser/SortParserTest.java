package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.grupozap.search.api.model.query.Item;
import java.util.List;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class SortParserTest {

  private final SortParser sortParser;

  public SortParserTest() {
    this.sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());
  }

  @Test
  public void testNearSort() {
    var sort = sortParser.parse("field NEAR [42.0,-74.0]");
    assertEquals("field ASC [42.0, -74.0]", sort.toString());
  }

  @Test
  public void testNestedNearSort() {
    var sort = sortParser.parse("field1.field2 NEAR [42.0,-74.0]");
    assertEquals("field1.field2 ASC [42.0, -74.0]", sort.toString());
  }

  @Test
  public void testOneSort() {
    var sort = sortParser.parse("field ASC");
    assertEquals("field ASC", sort.toString());
  }

  @Test
  public void testOneSortWithDefaultOrder() {
    var sort = sortParser.parse("field");
    assertEquals("field ASC", sort.toString());
  }

  @Test
  public void testMultipleSortWithDefaultOrder() {
    var sort = sortParser.parse("field1, field2");
    assertEquals("field1 ASC field2 ASC", sort.toString());
  }

  @Test
  public void testOneNestedFieldSort() {
    var sort = sortParser.parse("field.field2.field3 ASC");
    assertEquals("field.field2.field3 ASC", sort.toString());
  }

  @Test
  public void testMultipleSort() {
    var sort = sortParser.parse("field ASC, field2 ASC, field3 DESC");
    assertEquals("field ASC field2 ASC field3 DESC", sort.toString());
  }

  @Test
  public void testMultipleNestedFieldSort() {
    var sort =
        sortParser.parse(
            "field.field2 ASC, field2.field3.field4 ASC, field3.field5.field6.field10 DESC");
    assertEquals(
        "field.field2 ASC field2.field3.field4 ASC field3.field5.field6.field10 DESC",
        sort.toString());
  }

  @Test
  public void testMultipleSortWithIndex() {
    List<Item> items =
        newArrayList(sortParser.parse("field ASC,field2 ASC,field3 DESC, field  ASC"));

    assertThat(items, hasSize(3));
    assertEquals("field ASC", items.get(0).toString());
    assertEquals("field2 ASC", items.get(1).toString());
    assertEquals("field3 DESC", items.get(2).toString());
  }

  @Test
  public void testFieldWithAlias() {
    var sort = sortParser.parse("field_before_alias ASC");
    assertEquals("field_after_alias ASC", sort.toString());
  }

  /** ********************** Sort Filter ********************** */
  @Test
  public void testSingleWithSortFilter() {
    List<Item> items = newArrayList(sortParser.parse("field ASC sortFilter:field2 EQ \"ALPHA\""));

    assertThat(items, hasSize(1));
    assertEquals("field ASC (field2 EQUAL \"ALPHA\")", items.get(0).toString());
  }

  @Test
  public void testNearWithSortFilter() {
    List<Item> items =
        newArrayList(sortParser.parse("field NEAR [42.0,-74.0] sortFilter:field2 EQ \"BETA\""));

    assertThat(items, hasSize(1));
    assertEquals("field ASC [42.0, -74.0] (field2 EQUAL \"BETA\")", items.get(0).toString());
  }

  @Test
  public void testSingleWithSortFilterWithSpaces() {
    var sortClause =
        String.join(
            ",",
            "field1 ASC sortFilter:  filter1 EQ \"ALPHA\"",
            "field2 ASC  sortFilter :filter2 EQ \"BETA\"",
            "field3 ASC    sortFilter  :  filter3 EQ \"GAMMA\"",
            "fieldN ASC sortFilter   :    filterM EQ \"OMEGA\"");

    System.out.println(sortClause);
    List<Item> items = newArrayList(sortParser.parse(sortClause));

    assertThat(items, hasSize(4));
    assertEquals("field1 ASC (filter1 EQUAL \"ALPHA\")", items.get(0).toString());
    assertEquals("field2 ASC (filter2 EQUAL \"BETA\")", items.get(1).toString());
    assertEquals("field3 ASC (filter3 EQUAL \"GAMMA\")", items.get(2).toString());
    assertEquals("fieldN ASC (filterM EQUAL \"OMEGA\")", items.get(3).toString());
  }

  @Test(expected = ParserException.class)
  public void testSingleWithSortFilterWithoutColon() {
    sortParser.parse("field ASC sortFilter field2 EQ \"ALPHA\"");
  }

  @Test(expected = ParserException.class)
  public void testSingleWithSortFilterWithoutPrefix() {
    sortParser.parse("field ASC field2 EQ \"ALPHA\"");
  }

  @Test(expected = ParserException.class)
  public void testSingleWithSortFilterWithInvalidQuery() {
    sortParser.parse("field ASC sortFilter:field2 \"ALPHA\"");
  }

  @Test(expected = ParserException.class)
  public void testOneSortError() {
    sortParser.parse("field DSC");
  }

  @Test(expected = ParserException.class)
  public void testOneSortPrecedenceError() {
    sortParser.parse("ASC field DESC");
  }

  @Test(expected = ParserException.class)
  public void testSpaceSortError() {
    sortParser.parse(" ");
  }

  @Test(expected = ParserException.class)
  public void testNearSortWithMultipleCoordinates() {
    sortParser.parse("field NEAR [[42.0,-74.0], [30.0, -40.0]]");
  }
}
