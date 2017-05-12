package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

import java.util.List;

public class ValueParser {

    private static Terminals OPERATORS = Terminals.operators(","); // only one operator supported (for IN)
    private static Parser<?> VALUE_TOKENIZER = Parsers.or(
            Terminals.DecimalLiteral.TOKENIZER,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
    );
    private static Parser<String> VALUE_SYNTATIC_PARSER = Parsers.or(
            Terminals.DecimalLiteral.PARSER,
            Terminals.StringLiteral.PARSER
    );
    private static Parser<?> TOKENIZER = Parsers.or(OPERATORS.tokenizer(), VALUE_TOKENIZER); // tokenizes the OPERATORS and values
    private static Parser<List<String>> LIST_PARSER = Parsers.between(
            Scanners.isChar('['),
            VALUE_SYNTATIC_PARSER.sepBy(OPERATORS.token(",")).from(TOKENIZER, Scanners.WHITESPACES.skipMany()),
            Scanners.isChar(']')
    );
//    private static Parser<Void> IN_PARSER = Parsers.sequence(Scanners.WHITESPACES.skipAtLeast(1), Scanners.isChar('I'), Scanners.isChar('N'), Scanners.WHITESPACES.skipAtLeast(1)); // FIXME: "IN"

    private static Parser<Value> SINGLE_VALUE_PARSER = VALUE_SYNTATIC_PARSER.from(VALUE_TOKENIZER, Scanners.WHITESPACES.skipMany()).map(Value::new);
    private static Parser<Value> MULTI_VALUE_PARSER = LIST_PARSER.map(Value::new);

    static Parser<Value> getSingle() {
        return SINGLE_VALUE_PARSER;
    }

    static Parser<Value> getMulti() {
        return MULTI_VALUE_PARSER;
    }

}
