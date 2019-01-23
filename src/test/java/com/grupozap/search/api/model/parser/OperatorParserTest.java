package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.query.LogicalOperator.AND;
import static com.grupozap.search.api.model.query.RelationalOperator.EQUAL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import org.jparsec.error.ParserException;
import org.junit.Test;

public class OperatorParserTest {

  private final OperatorParser operatorParser = new OperatorParser();

  @Test
  public void testRelationalEqualsOperator() {
    var ops =
        of(":", "=", "EQ")
            .map(operatorParser.getRelationalOperatorParser()::parse)
            .collect(toList());

    assertThat(ops, hasSize(3));
    assertThat(ops, everyItem(equalTo(EQUAL)));
  }

  @Test(expected = ParserException.class)
  public void testRelationalInvalidOperator() {
    operatorParser.getRelationalOperatorParser().parse("?");
  }

  @Test
  public void testLogicalAndOperator() {
    var ops =
        of("AND", "&&").map(operatorParser.getLogicalOperatorParser()::parse).collect(toList());

    assertThat(ops, hasSize(2));
    assertThat(ops, everyItem(equalTo(AND)));
  }

  @Test(expected = ParserException.class)
  public void testLogicalInvalidOperator() {
    operatorParser.getLogicalOperatorParser().parse("?");
  }
}
