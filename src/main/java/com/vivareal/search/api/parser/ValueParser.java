package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

public class ValueParser {

    public static final Parser<Value> STRING_PARSER = Parsers.or(
            Scanners.DECIMAL,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
            Scanners.IDENTIFIER)
        .map(Value::new);

    public static Parser<Value> get() {
        return STRING_PARSER;
    }

}
