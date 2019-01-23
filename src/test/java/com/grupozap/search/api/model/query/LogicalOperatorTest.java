package com.grupozap.search.api.model.query;

import static com.grupozap.search.api.model.query.LogicalOperator.get;
import static com.grupozap.search.api.model.query.LogicalOperator.getOperators;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;
import org.junit.Test;

public class LogicalOperatorTest {

  @Test
  public void testGetOperators() {
    var operators = getOperators();
    assertNotNull(operators);
    assertTrue(operators.length > 0);
  }

  @Test
  public void testGetOperatorBySymbol() {
    Stream.of(getOperators()).forEach(operatorId -> assertNotNull(get(operatorId)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetInvalidOperator() {
    get("NonExistent");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullInvalidOperator() {
    get(null);
  }
}
