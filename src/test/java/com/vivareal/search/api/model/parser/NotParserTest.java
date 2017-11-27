package com.vivareal.search.api.model.parser;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotParserTest {

    private NotParser notParser = new NotParser();

    @Test
    public void testNot() {
        Boolean not = notParser.get().parse("NOT ");
        assertTrue(not);
    }

    @Test
    public void testWithoutNot() {
        Boolean not = notParser.get().parse("");
        assertFalse(not);
    }

    @Test
    public void testNotWithSpaces() {
        Boolean not = notParser.get().parse("    NOT   ");
        assertTrue(not);
    }
}
