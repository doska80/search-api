package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueParserTest {

    @Test
    public void testUnquotedString() {
        String unparsed = "unquoted";
        Parser<String> parser = ValueParser.get();
        String parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed);
    }

    @Test(expected = ParserException.class)
    public void testWrongUnquotedString() {
        Parser<String> parser = ValueParser.get();
        parser.parse("broken unquoted");
    }

    @Test
    public void testSingleQuotedString() {
        String unparsed = "'single-quoted and with a lot of spaces'";
        Parser<String> parser = ValueParser.get();
        String parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed);
    }

    @Test
    public void testDoubleQuotedString() {
        String unparsed = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Parser<String> parser = ValueParser.get();
        String parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed);
    }

}