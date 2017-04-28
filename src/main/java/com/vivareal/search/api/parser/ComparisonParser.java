package com.vivareal.search.api.parser;

import com.vivareal.search.api.model.query.Expression;
import org.jparsec.Parser;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Tokens;

public class ComparisonParser {

    protected static final Terminals OPERATORS = Terminals.operators(new String[] { ":", "=", "EQ", "NEQ", "GT", "LT", "GTE", "LTE", "(", ")", "[", "]", ",", "<>" });
    protected static final Parser<Comparison> OPERATOR_MAPPER = Terminals.fragment(Tokens.Tag.RESERVED).map(Comparison::get).cast();
    protected static final Parser<Comparison> OPERATOR_PARSER = OPERATOR_MAPPER.from(OPERATORS.tokenizer(), Scanners.WHITESPACES.optional(null)).cast();

    public static Parser<Comparison> get() {
        return OPERATOR_PARSER;
    }


}
