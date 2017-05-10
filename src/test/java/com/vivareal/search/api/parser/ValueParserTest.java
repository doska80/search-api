package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueParserTest {

    @Test
    public void testUnquotedString() {
        String unparsed = "unquoted";
        Parser<Value> parser = ValueParser.getSimple();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getFirstContent());
    }

    @Test(expected = ParserException.class)
    public void testWrongUnquotedString() {
        Parser<Value> parser = ValueParser.getSimple();
        parser.parse("broken unquoted");
    }

    @Test
    public void testSingleQuotedString() {
        String unparsed = "'single-quoted and with a lot of spaces'";
        Parser<Value> parser = ValueParser.getSimple();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getFirstContent());
    }

    @Test
    public void testDoubleQuotedString() {
        String unparsed = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Parser<Value> parser = ValueParser.getSimple();
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getFirstContent());
    }

    @Test
    public void testUsingIN() {
        String unparsed = "[1,'2',\"3\",'4 with spaces']";
        Parser<?> parser = ValueParser.get();
        Object parsed = parser.parse(unparsed);
        System.out.println(parsed);
    }

}