package com.grupozap.search.api.model.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;
import org.junit.Test;

public class OrderOperatorTest {

  @Test
  public void testGetOperators() {
    var operators = OrderOperator.getOperators();
    assertNotNull(operators);
    assertTrue(operators.length > 0);
  }

  @Test
  public void testGetOperatorBySymbol() {
    Stream.of(OrderOperator.getOperators())
        .forEach(operatorId -> assertNotNull(OrderOperator.get(operatorId)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetInvalidOperator() {
    OrderOperator.get("NonExistent");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullInvalidOperator() {
    OrderOperator.get(null);
  }
}
