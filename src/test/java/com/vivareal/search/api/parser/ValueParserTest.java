package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueParserTest {
    private static final Parser<Value> parser = ValueParser.get();

    @Test
    public void testInteger() {
        String unparsed = "123456";
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getContents(0));
    }

    @Test
    public void testFloat() {
        String unparsed = "123.456";
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed, parsed.getContents(0));
    }

    @Test(expected = ParserException.class)
    public void testUnquotedString() {
        String unparsed = "unquoted";
        parser.parse(unparsed);
    }

    @Test(expected = ParserException.class)
    public void testSpacedUnquotedString() {
        parser.parse("broken unquoted");
    }

    @Test
    public void testSingleQuotedString() {
        String unparsed = "'single-quoted and with a lot of spaces'";
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testDoubleQuotedString() {
        String unparsed = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Value parsed = parser.parse(unparsed);
        assertEquals(unparsed.substring(1, unparsed.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testUsingIN() {
        String unparsed = "[   1.2 ,'2   ',          3    ,   \"   4   \"     ]";
        Value parsed = parser.parse(unparsed);
        assertEquals("[\"1.2\", \"2   \", \"3\", \"   4   \"]", parsed.toString());
    }

    @Test(expected = ParserException.class)
    public void testUnquotedUsingIN() {
        String unparsed = "[   1.2 ,'2   ',          3    ,   \"   4   \"  , error   ]";
        parser.parse(unparsed);
    }

    @Test
    public void testNullValue() {
        Value nullValue = parser.parse("NULL");
        Value nullValueLowerCase = parser.parse("null");
        assertEquals(nullValue, Value.NULL_VALUE);
        assertEquals(nullValueLowerCase, Value.NULL_VALUE);
        assertEquals(nullValue, nullValueLowerCase);
    }

    @Test
    public void testBooleanFalseValue() {
        Value actual = new Value(false);

        Value falseValue = parser.parse("FALSE");
        assertEquals(falseValue, actual);
        assertEquals(falseValue, new Value(Boolean.FALSE));

        Value falseValueLowerCase = parser.parse("false");
        assertEquals(falseValueLowerCase, actual);
        assertEquals(falseValue, falseValueLowerCase);
    }

    @Test
    public void testBooleanTrueValue() {
        Value actual = new Value(true);

        Value trueValue = parser.parse("TRUE");
        assertEquals(trueValue, actual);
        assertEquals(trueValue, new Value(Boolean.TRUE));

        Value trueValueLowerCase = parser.parse("true");
        assertEquals(trueValueLowerCase, actual);
        assertEquals(trueValue, trueValueLowerCase);
    }
}