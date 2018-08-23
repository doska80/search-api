package com.grupozap.search.api.model.parser;

import static org.jparsec.Parsers.between;
import static org.jparsec.Parsers.or;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Terminals.fragment;
import static org.jparsec.Terminals.operators;

import com.grupozap.search.api.model.query.LogicalOperator;
import com.grupozap.search.api.model.query.OrderOperator;
import com.grupozap.search.api.model.query.RelationalOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jparsec.*;
import org.springframework.stereotype.Component;

@Component
public class OperatorParser {

  private final Parser<LogicalOperator> logicalOperatorParser =
      get(LogicalOperator::getOperators, LogicalOperator::get, "logical operator");

  private final Parser<RelationalOperator> relationalOperatorParser =
      get(RelationalOperator::getOperators, RelationalOperator::get, "relational operator");

  private final Parser<OrderOperator> orderOperatorParser =
      get(OrderOperator::getOperators, OrderOperator::get, "order operator");

  private <T> Parser<T> get(Supplier<String[]> operators, Function<String, T> getFn, String label) {
    Terminals OPERATORS = operators(operators.get());
    Parser<T> OPERATOR_MAPPER = fragment(Tokens.Tag.RESERVED).label(label).map(getFn);
    return OPERATOR_MAPPER.from(OPERATORS.tokenizer(), WHITESPACES.optional(null)).label(label);
  }

  Parser<String> exact(RelationalOperator operator) {
    return between(
            WHITESPACES.skipMany(),
            or(operator.getAlias().stream().map(Scanners::string).toArray(Parser[]::new)),
            WHITESPACES.skipMany())
        .retn(operator.name());
  }

  Parser<String> exact(RelationalOperator... operator) {
    return Parsers.or(Stream.of(operator).map(this::exact).toArray(Parser[]::new));
  }

  Parser<LogicalOperator> getLogicalOperatorParser() {
    return logicalOperatorParser;
  }

  Parser<RelationalOperator> getRelationalOperatorParser() {
    return relationalOperatorParser;
  }

  Parser<OrderOperator> getOrderOperatorParser() {
    return orderOperatorParser;
  }
}
