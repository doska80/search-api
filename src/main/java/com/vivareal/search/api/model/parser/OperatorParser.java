package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.LogicalOperator;
import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.query.RelationalOperator;
import org.jparsec.Parser;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Tokens;

import java.util.function.Function;
import java.util.function.Supplier;

public class OperatorParser {

    public static final Parser<LogicalOperator> LOGICAL_OPERATOR_PARSER = get(LogicalOperator::getOperators, LogicalOperator::get, "logical operator");

    public static final Parser<RelationalOperator> RELATIONAL_OPERATOR_PARSER = get(RelationalOperator::getOperators, RelationalOperator::get, "relational operator");

    public static final Parser<OrderOperator> ORDER_OPERATOR_PARSER = get(OrderOperator::getOperators, OrderOperator::get, "order operator");

    private static <T> Parser<T> get(Supplier<String[]> operators, Function<String, T> getFn, String label) {
        Terminals OPERATORS = Terminals.operators(operators.get());
        Parser<T> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).label(label).map(getFn);
        return OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).label(label);
    }
}
