package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class QueryParserTest {

    Parser<List<QueryFragment>> parser = QueryParser.get();

    @Test
    public void simpleTest() {
        List<QueryFragment> query = parser.parse("field1 EQ value1 AND field2 NE value2 OR field3 GT 123");
        for (QueryFragment fragment: query) {
            System.out.println(fragment);
        }
    }


}