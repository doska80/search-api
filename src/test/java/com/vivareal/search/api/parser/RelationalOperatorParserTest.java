package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.*;

public class RelationalOperatorParserTest {

    @Test
    public void testEqualitySymbol() {
        Parser<RelationalOperator> parser = RelationalOperatorParser.get();
        String[] validEqualitySymbols = new String[]{ ":", "=", "EQ" };
        for (String equalitySymbol: validEqualitySymbols) {
            RelationalOperator equalEnum = parser.parse(equalitySymbol);
            assertEquals(equalEnum, RelationalOperator.EQUAL);
        }
    }

}