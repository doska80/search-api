package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

public class ValueParser {

    public static final Parser<String> STRING_PARSER = Parsers.or(Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER, Scanners.IDENTIFIER);

    public static Parser<String> get() {
        return STRING_PARSER;
    }

}
