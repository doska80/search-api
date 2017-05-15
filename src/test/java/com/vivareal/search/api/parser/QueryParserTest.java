package com.vivareal.search.api.parser;

import com.google.common.base.Joiner;
import org.jparsec.Parser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    Parser<List<QueryFragment>> parser = QueryParser.get();
    Parser<List<QueryFragment>> recursiveParser = QueryParser.getRecursive();

    @Test
    public void nonRecursiveMultipleConditionsTest() {
        List<QueryFragment> query = parser.parse("field1 EQ 'value1' AND field2 NE 'value2' OR field3 GT 123 AND field4 NE 42");
        assertEquals("field1 EQUAL value1 AND field2 DIFFERENT value2 OR field3 GREATER 123 AND field4 DIFFERENT 42", Joiner.on(' ').join(query));
    }

    @Test
    @Ignore
    public void oneRecursionAndMultipleConditionsTest() {
        List<QueryFragment> query = recursiveParser.parse("field1 EQ 'value1' AND (field2 NE 'value2' OR field3 GT 123) AND field4 NE 42");
        assertEquals("field1 EQUAL value1 AND (field2 DIFFERENT value2 OR field3 GREATER 123) AND field4 DIFFERENT 42", Joiner.on(' ').join(query));
//        recursiveParser.parse("field1 EQ value1 AND (field2 NE value2 OR field3 GT 123) AND field4 NE 42");
    }

}