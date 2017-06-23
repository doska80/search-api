package com.vivareal.search.api.model.parser;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotParserTest {

    @Test
    public void testNot() {
        Boolean not = NotParser.get().parse("NOT ");
        assertTrue(not);
    }

    @Test
    public void testWithoutNot() {
        Boolean not = NotParser.get().parse("");
        assertFalse(not);
    }

    @Test
    public void testNotWithSpaces() {
        Boolean not = NotParser.get().parse("    NOT   ");
        assertTrue(not);
    }
}
