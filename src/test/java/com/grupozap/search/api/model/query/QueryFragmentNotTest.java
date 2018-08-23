package com.grupozap.search.api.model.query;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.junit.Test;

public class QueryFragmentNotTest {

  @Test
  public void validTrueNotValue() {
    QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(true));
    assertEquals("NOT", queryFragmentNot.toString());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(true));
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(false));
  }

  @Test
  public void validFalseNotValue() {
    QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(false));
    assertEquals("", queryFragmentNot.toString());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(true));
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(false));
  }

  @Test
  public void nullNotValue() {
    QueryFragmentNot queryFragmentNot = new QueryFragmentNot(singletonList(null));
    assertEquals("", queryFragmentNot.toString());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(true));
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(false));
  }

  @Test
  public void emptyNotValue() {
    QueryFragmentNot queryFragmentNot = new QueryFragmentNot(new ArrayList<>());
    assertEquals("", queryFragmentNot.toString());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames());
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(true));
    assertEquals(newHashSet(), queryFragmentNot.getFieldNames(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void multipleNotValue() {
    QueryFragmentNot queryFragmentNot = new QueryFragmentNot(asList(true, true));
    assertEquals("", queryFragmentNot.toString());
  }
}
