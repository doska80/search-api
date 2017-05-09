package com.vivareal.search.api.parser;

import com.google.common.base.Joiner;
import org.jparsec.Parser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    Parser<List<QueryFragment>> parser = QueryParser.get();

    @Test
    public void nonRecursiveMultipleConditionsTest() {
        List<QueryFragment> query = parser.parse("field1 EQ value1 AND field2 NE value2 OR field3 GT 123");
        assertEquals("field1 EQUAL value1 AND field2 DIFFERENT value2 OR field3 GREATER 123", Joiner.on(' ').join(query));
    }


}