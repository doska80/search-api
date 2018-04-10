package com.vivareal.search.api.model.parser;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.vivareal.search.api.model.query.Item;
import com.vivareal.search.api.model.query.Sort;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class SortParserTest {

  private SortParser sortParser;

  public SortParserTest() {
    this.sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());
  }

  @Test
  public void testNearSort() {
    Sort sort = sortParser.parse("field NEAR [42.0,-74.0]");
    assertEquals("field ASC [42.0, -74.0]", sort.toString());
  }

  @Test
  public void testNestedNearSort() {
    Sort sort = sortParser.parse("field1.field2 NEAR [42.0,-74.0]");
    assertEquals("field1.field2 ASC [42.0, -74.0]", sort.toString());
  }

  @Test
  public void testOneSort() {
    Sort sort = sortParser.parse("field ASC");
    assertEquals("field ASC", sort.toString());
  }

  @Test
  public void testOneSortWithDefaultOrder() {
    Sort sort = sortParser.parse("field");
    assertEquals("field ASC", sort.toString());
  }

  @Test
  public void testOneNestedFieldSort() {
    Sort sort = sortParser.parse("field.field2.field3 ASC");
    assertEquals("field.field2.field3 ASC", sort.toString());
  }

  @Test
  public void testMultipleSort() {
    Sort sort = sortParser.parse("field ASC, field2 ASC, field3 DESC");
    assertEquals("field ASC field2 ASC field3 DESC", sort.toString());
  }

  @Test
  public void testMultipleNestedFieldSort() {
    Sort sort =
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
    String sortClause =
        Stream.of(
                "field1 ASC sortFilter:  filter1 EQ \"ALPHA\"",
                "field2 ASC  sortFilter :filter2 EQ \"BETA\"",
                "field3 ASC    sortFilter  :  filter3 EQ \"GAMMA\"",
                "fieldN ASC sortFilter   :    filterM EQ \"OMEGA\"")
            .collect(Collectors.joining(","));

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
