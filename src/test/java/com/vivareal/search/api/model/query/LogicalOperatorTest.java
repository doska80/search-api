package com.vivareal.search.api.model.query;

import org.junit.Test;

import java.util.stream.Stream;

import static com.vivareal.search.api.model.query.LogicalOperator.get;
import static com.vivareal.search.api.model.query.LogicalOperator.getOperators;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LogicalOperatorTest {

    @Test
    public void testGetOperators() {
        String[] operators = getOperators();
        assertNotNull(operators);
        assertTrue(operators.length > 0);
    }

    @Test
    public void testGetOperatorBySymbol() {
        Stream.of(getOperators()).forEach(operatorId -> assertNotNull(get(operatorId)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInvalidOperator() {
        get("Unexistent");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInvalidOperator() {
        get(null);
    }
}
