package com.grupozap.search.api.model.query;

import static com.vivareal.search.api.model.query.RelationalOperator.get;
import static com.vivareal.search.api.model.query.RelationalOperator.getOperators;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;
import org.junit.Test;

public class RelationalOperatorTest {

  @Test
  public void testGetOperators() {
    String[] operators = RelationalOperator.getOperators();
    assertNotNull(operators);
    assertTrue(operators.length > 0);
  }

  @Test
  public void testGetOperatorBySymbol() {
    Stream.of(RelationalOperator.getOperators()).forEach(operatorId -> assertNotNull(
        RelationalOperator.get(operatorId)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetInvalidOperator() {
    RelationalOperator.get("NonExistent");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullInvalidOperator() {
    RelationalOperator.get(null);
  }
}
