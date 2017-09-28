package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Filter;
import com.vivareal.search.api.model.query.Value;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.*;

public class FilterParserTest {
    private static final Parser<Filter> parser = FilterParser.get();

    @Test
    public void testSingleExpressionWithDoubleQuotes() {
        parser.parse("field=\"value\"");
    }

    @Test
    public void testSingleExpressionWithNumberField() {
        Filter filter = parser.parse("field10=10");
        assertEquals("field10 EQUAL 10", filter.toString());
    }

    @Test
    public void testSingleExpressionWithSpacesAndSingleQuotes() {
        Filter filter = parser.parse("field.field2 = 'space value'");
        assertEquals("field.field2 EQUAL \"space value\"", filter.toString());
    }

    @Test
    public void testSingleExpressionWithINAndSpaces() {
        Filter filter = parser.parse("list IN [\"a\", 'b']");
        assertEquals("list IN [\"a\", \"b\"]", filter.toString());
    }

    @Test
    public void testEqualsLikeAsIN() {
        Filter filter = parser.parse("list = [\"a\", 'b']");
        assertEquals("list EQUAL [\"a\", \"b\"]", filter.toString());
    }

    @Test(expected = ParserException.class)
    public void testINLikeAsEquals() {
        parser.parse("list IN \"a\", \"b\"");
    }

    @Test
    public void testFilterEmpty() {
        Filter filter = parser.parse("field = \"\"");
        assertEquals("field EQUAL \"\"", filter.toString());
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterNull() {
        Filter filter = parser.parse("field = NULL");
        assertEquals("field EQUAL NULL", filter.toString());
        assertTrue(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterQuotedNull() {
        Filter filter = parser.parse("field = 'NULL'");
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterBooleanTrue() {
        Filter filterTrue = parser.parse("field = TRUE");
        Filter filterTrueLowerCase = parser.parse("field = true");
        assertEquals("field EQUAL true", filterTrue.toString());
        assertEquals(filterTrue.toString(), filterTrueLowerCase.toString());
    }

    @Test
    public void testFilterBooleanFalse() {
        Filter filterFalse = parser.parse("field = FALSE");
        Filter filterTrueLowerCase = parser.parse("field = false");
        assertEquals("field EQUAL false", filterFalse.toString());
        assertEquals(filterFalse.toString(), filterTrueLowerCase.toString());
    }

    @Test(expected = ParserException.class)
    public void testInvalidRelationalViewports() {
        parser.parse("address.geoLocation EQ [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidSingleViewports() {
        parser.parse("address.geoLocation VIEWPORT [[-23.5534103,-46.6597479]]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidMultipleViewports() {
        parser.parse("address.geoLocation VIEWPORT [[-46.6597479],[-23.5534103,-46.6597479]]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidMultipleViewportsOnSecond() {
        parser.parse("address.geoLocation VIEWPORT [[-23.5534103,-46.6597479],[-23.5534103]]");
    }

    @Test(expected = ParserException.class)
    public void testInvalidViewportSingleValue() {
        parser.parse("address.geoLocation VIEWPORT \"df\"");
    }

    @Test(expected = ParserException.class)
    public void testEmptyViewports() {
        parser.parse("address.geoLocation VIEWPORT");
        parser.parse("address.geoLocation VIEWPORT []");
        parser.parse("address.geoLocation VIEWPORT [,]");
    }

    @Test
    public void testMultipleViewports() {
        String value = "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
        Filter viewport = parser.parse(value);
        assertEquals("address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]", viewport.toString());
    }

    @Test(expected = ParserException.class)
    public void testSingleViewports() {
        String value = "address.geoLocation VIEWPORT [[-23.5534103,-46.6597479]]";
        Filter viewport = parser.parse(value);
        assertEquals("address.geoLocation VIEWPORT [-23.5534103, -46.6597479]", viewport.toString());
    }

    @Test
    public void testMultipleViewportsWithAlias() {
        String value = "address.geoLocation @ [[-23.5534103,-46.6597479],[-23.5534103,-46.6597479]]";
        Filter viewport = parser.parse(value);
        assertEquals("address.geoLocation VIEWPORT [[-23.5534103, -46.6597479], [-23.5534103, -46.6597479]]", viewport.toString());
    }

    @Test
    public void testSingleLike() {
        String value = "field LIKE '% \\% _ \\_ * \\n ? \\x'";
        Filter like = parser.parse(value);
        assertEquals("field LIKE \"* % ? _ \\* \n \\? \\x\"", like.toString());
    }

    @Test
    public void testSingleRange() {
        String value = "field RANGE [\"a\", 5]";
        Filter like = parser.parse(value);
        assertEquals("field RANGE [\"a\", 5]", like.toString());
    }

    @Test(expected = ParserException.class)
    public void testSingleInvalidRange() {
        String value = "field RANGE [1,]";
        parser.parse(value);
    }
}
