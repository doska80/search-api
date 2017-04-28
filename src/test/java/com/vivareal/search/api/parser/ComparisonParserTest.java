package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComparisonParserTest {

    @Test
    public void testEqualitySymbol() {
        Parser<Comparison> parser = ComparisonParser.get();
        String[] validEqualitySymbols = new String[]{ ":", "=", "EQ" };
        for (String equalitySymbol: validEqualitySymbols) {
            Comparison equalEnum = parser.parse(equalitySymbol);
            assertEquals(equalEnum, Comparison.EQUAL);
        }
    }

}