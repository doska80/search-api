package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Sort;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SortParserTest {

    private SortParser sortParser;

    public SortParserTest() {
        FieldParser fieldParser = new FieldParser(new NotParser());
        OperatorParser operatorParser = new OperatorParser();
        this.sortParser = new SortParser(fieldParser, operatorParser);
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
        Sort sort = sortParser.parse("field.field2 ASC, field2.field3.field4 ASC, field3.field5.field6.field10 DESC");
        assertEquals("field.field2 ASC field2.field3.field4 ASC field3.field5.field6.field10 DESC", sort.toString());
    }

    @Test
    public void testMultipleSortWithIndex() {
        List<Sort.Item> items = newArrayList(sortParser.parse("field ASC,field2 ASC,field3 DESC, field  ASC"));

        assertThat(items, hasSize(3));
        assertEquals("field ASC", items.get(0).toString());
        assertEquals("field2 ASC", items.get(1).toString());
        assertEquals("field3 DESC", items.get(2).toString());
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
}
