package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SortParserTest {

    @Test
    public void testOneSort() {
        Sort sort = SortParser.parse("field ASC");
        assertEquals("field ASC", sort.toString());
    }

    @Test
    public void testOneSortWithDefaultOrder() {
        Sort sort = SortParser.parse("field");
        assertEquals("field ASC", sort.toString());
    }

    @Test
    public void testOneNestedFieldSort() {
        Sort sort = SortParser.parse("field.field2.field3 ASC");
        assertEquals("field.field2.field3 ASC", sort.toString());
    }

    @Test
    public void testMultipleSort() {
        Sort sort = SortParser.parse("field ASC, field2 ASC, field3 DESC");
        assertEquals("field ASC field2 ASC field3 DESC", sort.toString());
    }

    @Test
    public void testMultipleNestedFieldSort() {
        Sort sort = SortParser.parse("field.field2 ASC, field2.field3.field4 ASC, field3.field5.field6.field10 DESC");
        assertEquals("field.field2 ASC field2.field3.field4 ASC field3.field5.field6.field10 DESC", sort.toString());
    }

    @Test
    public void testMultipleSortWithIndex() {
        Sort sort = SortParser.parse("field ASC,field2 ASC,field3 DESC");

        assertThat(sort, hasSize(3));
        assertEquals("field ASC", sort.get(0).toString());
        assertEquals("field2 ASC", sort.get(1).toString());
        assertEquals("field3 DESC", sort.get(2).toString());
    }

    @Test(expected = ParserException.class)
    public void testOneSortError() {
        SortParser.parse("field DSC");
    }

    @Test(expected = ParserException.class)
    public void testOneSortPrecedenceError() {
        SortParser.parse("ASC field DESC");
    }

    @Test(expected = ParserException.class)
    public void testSpaceSortError() {
        SortParser.parse(" ");
    }
}
