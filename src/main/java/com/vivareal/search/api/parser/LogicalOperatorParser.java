package com.vivareal.search.api.parser;

import org.jparsec.*;

public class LogicalOperatorParser {
    private static final Terminals OPERATORS = Terminals.operators(LogicalOperator.getOperators());
    private static final Parser<LogicalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(LogicalOperator::get);
    private static final Parser<LogicalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES);

    static Parser<LogicalOperator> get() {
        return OPERATOR_PARSER;
    }
}
