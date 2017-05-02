package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Tokens;

public class RelationalOperatorParser {

    protected static final Terminals OPERATORS = Terminals.operators(RelationalOperator.getOperators());
    protected static final Parser<RelationalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(RelationalOperator::get).cast();
    protected static final Parser<RelationalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).cast();

    public static Parser<RelationalOperator> get() {
        return OPERATOR_PARSER;
    }


}
