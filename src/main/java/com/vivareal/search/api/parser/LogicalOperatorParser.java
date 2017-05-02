package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Tokens;

public class LogicalOperatorParser {

    protected static final Terminals OPERATORS = Terminals.operators(LogicalOperator.getOperators());
    protected static final Parser<LogicalOperator> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(LogicalOperator::get).cast();
    protected static final Parser<LogicalOperator> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).cast();

    public static Parser<LogicalOperator> get() {
        return OPERATOR_PARSER;
    }


}
