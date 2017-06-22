package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueParserTest {

    @Test
    public void testInteger() {
        String unparsed = "123456";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getContents(0));
    }

    @Test
    public void testFloat() {
        String unparsed = "123.456";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getContents(0));
    }

    @Test(expected = ParserException.class)
    public void testUnquotedString() {
        String unparsed = "unquoted";
        Parser<Value> parser = ValueParser.get();
        parser.parse(unparsed);
    }

    @Test(expected = ParserException.class)
    public void testSpacedUnquotedString() {
        Parser<Value> parser = ValueParser.get();
        parser.parse("broken unquoted");
    }

    @Test
    public void testSingleQuotedString() {
        String unparsed = "'single-quoted and with a lot of spaces'";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testDoubleQuotedString() {
        String unparsed = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testUsingIN() {
        String unparsed = "[   1.2 ,'2   ',          3    ,   \"   4   \"     ]";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals("[\"1.2\", \"2   \", \"3\", \"   4   \"]", parsed.toString());
    }

    @Test(expected = ParserException.class)
    public void testUnquotedUsingIN() {
        String unparsed = "[   1.2 ,'2   ',          3    ,   \"   4   \"  , error   ]";
        Parser<Value> parser = ValueParser.get();
        parser.parse(unparsed);
    }

    @Test
    public void testNullValue() {
        Parser<Value> parser = ValueParser.get();
        Value nullValue = parser.parse("NULL");
        Value nullValueLowerCase = parser.parse("null");
        assertEquals(nullValue, Value.NULL_VALUE);
        assertEquals(nullValueLowerCase, Value.NULL_VALUE);
        assertEquals(nullValue, nullValueLowerCase);
    }
}