package com.vivareal.search.api.parser;

import org.jparsec.*;

public class RelationalOperatorParser {
    private static final Terminals OPERATORS = Terminals.operators(RelationalOperator.getOperators());
    private static final Parser<RelationalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(RelationalOperator::get);
    private static final Parser<RelationalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null));

    static Parser<RelationalOperator> get() {
        return OPERATOR_PARSER;
    }
}