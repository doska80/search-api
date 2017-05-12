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
        assertEquals(unparsed, parsed.getFirstContent());
    }

    @Test
    public void testFloat() {
        String unparsed = "123.456";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getFirstContent());
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
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getFirstContent());
    }

    @Test
    public void testDoubleQuotedString() {
        String unparsed = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getFirstContent());
    }

    @Test
    public void testUsingIN() {
        String unparsed = "   IN   [   1.2 ,'2   ',          3    ,   \"   4   \"     ]";
        Parser<Value> parser = ValueParser.get();
        Value parsed = parser.parse(unparsed);
        assertEquals("[1.2, 2   , 3,    4   ]", parsed.toString());
    }

    @Test(expected = ParserException.class)
    public void testUnquotedUsingIN() {
        String unparsed = "   IN   [   1.2 ,'2   ',          3    ,   \"   4   \"  , error   ]";
        Parser<Value> parser = ValueParser.get();
        parser.parse(unparsed);
    }

}