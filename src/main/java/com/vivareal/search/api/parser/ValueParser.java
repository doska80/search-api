package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

import java.util.List;

public class ValueParser {

    public static final Parser<Value> SIMPLE_VALUE_PARSER = Parsers.or(
            Scanners.DECIMAL,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
            Scanners.IDENTIFIER)
        .cast();

//    public static final Parser<Value> MULTI_VALUE_PARSER = Parsers.between("[", SIMPLE_VALUE_PARSER, "]");

    public static final Parser<Value> VALUE_PARSER = Parsers.or(
            Scanners.DECIMAL,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
            Scanners.IDENTIFIER)
        .map(Value::new);

    public static Parser<Value> get() {
        return VALUE_PARSER;
    }

}
