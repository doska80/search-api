package com.vivareal.search.api.model.query;

import static com.google.common.collect.Sets.newHashSet;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.LogicalOperator.OR;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryFragmentOperatorTest {

  @Test
  public void validQueryFragmentOperatorAnd() {
    QueryFragmentOperator qfo = new QueryFragmentOperator(AND);
    assertEquals(AND, qfo.getOperator());
    assertEquals(newHashSet(), qfo.getFieldNames());
    assertEquals(newHashSet(), qfo.getFieldNames(true));
    assertEquals(newHashSet(), qfo.getFieldNames(false));
  }

  @Test
  public void validQueryFragmentOperatorOr() {
    QueryFragmentOperator qfo = new QueryFragmentOperator(OR);
    assertEquals(OR, qfo.getOperator());
    assertEquals(newHashSet(), qfo.getFieldNames());
    assertEquals(newHashSet(), qfo.getFieldNames(true));
    assertEquals(newHashSet(), qfo.getFieldNames(false));
  }
}
