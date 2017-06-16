package com.vivareal.search.api.parser;


import org.jparsec.*;

public class ValueParser {

    private static Terminals OPERATORS = Terminals.operators(",");
    private static Parser<?> VALUE_TOKENIZER = Parsers.or(
            Terminals.DecimalLiteral.TOKENIZER,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
    );

    private static Parser<?> TOKENIZER = Parsers.or(OPERATORS.tokenizer(), VALUE_TOKENIZER);

    private static Parser<String> VALUE_SYNTACTIC_PARSER = Parsers.or(
            Terminals.DecimalLiteral.PARSER,
            Terminals.StringLiteral.PARSER
    );

    private static Parser<Value> IN_VALUE_PARSER = Parsers
            .between(Scanners.isChar('['), VALUE_SYNTACTIC_PARSER.sepBy(OPERATORS.token(",")).from(TOKENIZER, Scanners.WHITESPACES.skipMany()), Scanners.isChar(']'))
            .map(Value::new);

    private static Parser<Value> NULL_VALUE_PARSER = Scanners.stringCaseInsensitive("NULL").retn(Value.NULL_VALUE);
    private static Parser<Value> VALUE_PARSER = Parsers.or(VALUE_SYNTACTIC_PARSER.from(VALUE_TOKENIZER, Scanners.WHITESPACES.skipMany()).map(Value::new), NULL_VALUE_PARSER);
    private static Parser<Value> VALUE_PARSER_WITH_IN = VALUE_PARSER.or(IN_VALUE_PARSER);

    static Parser<Value> get() {
        return VALUE_PARSER_WITH_IN;
    }
}