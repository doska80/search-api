package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

import java.util.Arrays;
import java.util.List;

public class ValueParser {

    static Terminals OPERATORS = Terminals.operators(","); // only one operator supported (for IN)
    static Parser<?> VALUE_TOKENIZER = Parsers.or(
            Terminals.DecimalLiteral.TOKENIZER,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
    );
    static Parser<String> VALUE_SYNTATIC_PARSER = Parsers.or(
            Terminals.DecimalLiteral.PARSER,
            Terminals.StringLiteral.PARSER
    );
    static Parser<?> TOKENIZER = Parsers.or(OPERATORS.tokenizer(), VALUE_TOKENIZER); // tokenizes the OPERATORS and values
    static Parser<List<String>> LIST_PARSER = Parsers.between(
            Scanners.isChar('['),
            VALUE_SYNTATIC_PARSER.sepBy(OPERATORS.token(",")).from(TOKENIZER, Scanners.WHITESPACES.skipMany()),
            Scanners.isChar(']')
    );
    static Parser<Void> IN_PARSER = Parsers.sequence(Scanners.WHITESPACES.skipAtLeast(1), Scanners.isChar('I'), Scanners.isChar('N'), Scanners.WHITESPACES.skipAtLeast(1)); // FIXME: "IN"

    static Parser<List<String>> SINGLE_VALUE_PARSER = VALUE_SYNTATIC_PARSER.from(VALUE_TOKENIZER, Scanners.WHITESPACES.skipMany()).map(Arrays::asList);
    static Parser<List<String>> MULTI_VALUE_PARSER = Parsers.sequence(IN_PARSER, LIST_PARSER);
    static Parser<Value> FULL_VALUE_PARSER = Parsers.or(MULTI_VALUE_PARSER, SINGLE_VALUE_PARSER).map(Value::new);

    static Parser<Value> get() {
        return FULL_VALUE_PARSER;
    }

}
