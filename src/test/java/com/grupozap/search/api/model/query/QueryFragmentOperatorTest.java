package com.grupozap.search.api.model.query;

import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.model.query.LogicalOperator.AND;
import static com.grupozap.search.api.model.query.LogicalOperator.OR;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryFragmentOperatorTest {

  private void validQueryFragmentOperator(LogicalOperator operator) {
    var qfo = new QueryFragmentOperator(operator);
    assertEquals(operator, qfo.getOperator());
    assertEquals(newHashSet(), qfo.getFieldNames());
    assertEquals(newHashSet(), qfo.getFieldNames(true));
    assertEquals(newHashSet(), qfo.getFieldNames(false));
  }

  @Test
  public void validQueryFragmentOperatorAnd() {
    validQueryFragmentOperator(AND);
  }

  @Test
  public void validQueryFragmentOperatorOr() {
    validQueryFragmentOperator(OR);
  }
}
