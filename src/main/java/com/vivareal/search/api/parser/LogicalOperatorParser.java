package com.vivareal.search.api.parser;

import org.jparsec.*;

public class LogicalOperatorParser {

    private static final Terminals OPERATORS = Terminals.operators(LogicalOperator.getOperators());
    private static final Parser<LogicalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(LogicalOperator::get).cast();
    private static final Parser<LogicalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.skipMany()).cast();

    static Parser<LogicalOperator> get() {
        return OPERATOR_PARSER;
    }

    /**
     * A Parser that recognizes a token identified by any of {@code names}.
     */
    static Parser<Token> getToken(String... names) {
        return OPERATORS.token(names);
    }

}
