package com.vivareal.search.api.parser;

import org.jparsec.*;

public class RelationalOperatorParser {

    static final Terminals OPERATORS = Terminals.operators(RelationalOperator.getOperators());
    static final Parser<RelationalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(RelationalOperator::get).cast();
    static final Parser<RelationalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).cast();

    static Parser<RelationalOperator> get() {
        return OPERATOR_PARSER;
    }

    /** A Parser that recognizes a token identified by any of {@code names}. */
    static Parser<Token> getToken(String... names) {
        return OPERATORS.token(names);
    }

}
