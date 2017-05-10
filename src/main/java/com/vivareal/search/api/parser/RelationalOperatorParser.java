package com.vivareal.search.api.parser;

import org.jparsec.*;

public class RelationalOperatorParser {

    protected static final Terminals OPERATORS = Terminals.operators(RelationalOperator.getOperators());
    protected static final Parser<RelationalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(RelationalOperator::get).cast();
    protected static final Parser<RelationalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).cast();

    public static Parser<RelationalOperator> get() {
        return OPERATOR_PARSER;
    }

    /** A Parser that recognizes a token identified by any of {@code names}. */
    public static Parser<Token> getToken(String... names) {
        return OPERATORS.token(names);
    }

}
