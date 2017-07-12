package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Value;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueParserTest {
    private static final Parser<Value> parser = ValueParser.get();

    private static final Parser<Value> parserViewport = ValueParser.Viewport.get();

    @Test
    public void testInteger() {
        String value = "123456";
        Value parsed = parser.parse(value);
        assertEquals(Integer.valueOf(value), parsed.getContents(0));
    }

    @Test
    public void testFloat() {
        String value = "123.456";
        Value parsed = parser.parse(value);
        assertEquals(Double.valueOf(value), parsed.getContents(0));
    }

    @Test(expected = ParserException.class)
    public void testUnquotedString() {
        parser.parse("unquoted");
    }

    @Test(expected = ParserException.class)
    public void testSpacedUnquotedString() {
        parser.parse("broken unquoted");
    }

    @Test
    public void testSingleQuotedString() {
        String value = "'single-quoted and with a lot of spaces'";
        Value parsed = parser.parse(value);
        assertEquals(value.substring(1, value.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testDoubleQuotedString() {
        String value = "\"single-quoted and with a lot of spaces and sôme spécial chars\"";
        Value parsed = parser.parse(value);
        assertEquals(value.substring(1, value.length() - 1), parsed.getContents(0));
    }

    @Test
    public void testUsingIN() {
        String value = "[1.2,'2',3,\"4\"]";
        Value parsed = parser.parse(value);
        assertEquals("[1.2, \"2\", 3, \"4\"]", parsed.toString());
    }

    @Test
    public void testUsingINWithSpaces() {
        String in = "[   1.2 ,'2   ',          3    ,   \"   4   \"     ]";
        Value parsed = parser.parse(in);
        assertEquals("[1.2, \"2\", 3, \"4\"]", parsed.toString());
    }

    @Test(expected = ParserException.class)
    public void testUnquotedUsingIN() {
        parser.parse("[   1.2 ,'2   ',          3    ,   \"   4   \"  , error   ]");
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

    @Test
    public void testSimpleNegativeDoubleIN() {
        String value = "[-23.5534103,-46.6597479]";
        Value viewport = parser.parse(value);
        assertEquals("[-23.5534103, -46.6597479]", viewport.toString());
    }

    @Test
    public void testMultipleNegativeDoubleIN() {
        String value = "[-23.5534103,-46.6597479;-23.5534103,-46.6597479]";
        Value viewport = parserViewport.parse(value);
        assertEquals("[[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]", viewport.toString());
    }

    @Test
    public void testNegativeDoubleValuesLikeIN() {
        String value = "[-23.5534103  ,   -46.6597479, -23.5534103,-46.6597479  ]";
        Value in = parser.parse(value);
        assertEquals("[-23.5534103, -46.6597479, -23.5534103, -46.6597479]", in.toString());
    }

    @Test(expected = ParserException.class)
    public void testInvalidFirstMultipleNegativeDoubleIN() {
        parserViewport.parse("[-23.5534103,-46.6597479;-23.5534103]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidSecondMultipleNegativeDoubleIN() {
        parserViewport.parse("[-46.6597479;-23.5534103,-46.6597479]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidMultipleNegativeDoubleIN() {
        parserViewport.parse("[-23.5534103,-46.6597479;-23.5534103,-46.6597479;-23.5534103,-46.6597479]");
    }
}
