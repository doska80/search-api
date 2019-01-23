package com.grupozap.search.api.model.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotParserTest {

  private final NotParser notParser = new NotParser();

  @Test
  public void testNot() {
    var not = notParser.get().parse("NOT ");
    assertTrue(not);
  }

  @Test
  public void testWithoutNot() {
    var not = notParser.get().parse("");
    assertFalse(not);
  }

  @Test
  public void testNotWithSpaces() {
    var not = notParser.get().parse("    NOT   ");
    assertTrue(not);
  }
}
