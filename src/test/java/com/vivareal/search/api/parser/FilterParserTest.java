package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterParserTest {

    @Test
    public void testSingleExpressionWithDoubleQuotes() {
        Parser<Filter> parser = FilterParser.get();
        parser.parse("pedrito=\"gringo\"");
    }

    @Test
    public void testSingleExpressionWithSpacesAndSingleQuotes() {
        Parser<Filter> parser = FilterParser.get();
        Filter filter = parser.parse("pedrito.colombiano = 'gringo mardito'");
        assertEquals("pedrito.colombiano EQUAL gringo mardito", filter.toString());
    }

    @Test
    public void testSingleExpressionWithINAndSpaces() {
        Parser<Filter> parser = FilterParser.get();
        Filter filter = parser.parse("pedrito IN [\"gringo\", 'mardito']");
        assertEquals("pedrito IN [\"gringo\", \"mardito\"]", filter.toString());
    }

    @Test
    public void testEqualsLikeAsIN() {
        Filter filter = FilterParser.get().parse("fato = [\"exato\", 'mezzatto']");
        assertEquals("fato EQUAL [\"exato\", \"mezzatto\"]", filter.toString());
    }

    @Test(expected = ParserException.class)
    public void testINLikeAsEquals() {
        FilterParser.get().parse("mezzatto IN \"exato\", \"correto\"");
    }

    @Test
    public void testFilterEmpty() {
        Filter filter = FilterParser.get().parse("sobrinho = \"\"");
        assertEquals("sobrinho EQUAL \"\"", filter.toString());
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterNull() {
        Filter filter = FilterParser.get().parse("sobrinho = NULL");
        assertEquals("sobrinho EQUAL NULL", filter.toString());
        assertTrue(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterQuotedNull() {
        Filter filter = FilterParser.get().parse("sobrinho = 'NULL'");
        assertFalse(filter.getValue().equals(Value.NULL_VALUE));
    }

    @Test
    public void testFilterNot() {
        Filter filter = FilterParser.get().parse("exato = \"mezzatto\"");
        assertEquals("exato EQUAL mezzatto", filter.toString());
        assertFalse(filter.isNot());

        Filter filterNot = FilterParser.get().parse("NOT exato = \"mezzatto\"");
        assertEquals("NOT exato EQUAL mezzatto", filterNot.toString());
        assertTrue(filterNot.isNot());
    }
}
