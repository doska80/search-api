package com.vivareal.search.api.parser;

import org.jparsec.*;

public class LogicalOperatorParser {

    protected static final Terminals OPERATORS = Terminals.operators(LogicalOperator.getOperators());
    protected static final Parser<LogicalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(LogicalOperator::get).cast();
    protected static final Parser<LogicalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.skipMany()).cast();

    public static Parser<LogicalOperator> get() {
        return OPERATOR_PARSER;
    }

    /** A Parser that recognizes a token identified by any of {@code names}. */
    public static Parser<Token> getToken(String... names) {
        return OPERATORS.token(names);
    }

}
