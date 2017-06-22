package com.vivareal.search.api.model.parser;


import com.vivareal.search.api.model.query.Value;
import org.jparsec.*;

import static org.jparsec.Terminals.*;

public class ValueParser {
    private static final Terminals OPERATORS = operators(",");
    private static final Parser<?> VALUE_TOKENIZER = Parsers.or(DecimalLiteral.TOKENIZER, StringLiteral.SINGLE_QUOTE_TOKENIZER, StringLiteral.DOUBLE_QUOTE_TOKENIZER);

    private static final Parser<?> TOKENIZER = Parsers.or(OPERATORS.tokenizer(), VALUE_TOKENIZER);

    private static final Parser<String> VALUE_SYNTACTIC_PARSER = Parsers.or(DecimalLiteral.PARSER, StringLiteral.PARSER);

    private static final Parser<Value> IN_VALUE_PARSER = Parsers
            .between(Scanners.isChar('['), VALUE_SYNTACTIC_PARSER.sepBy(OPERATORS.token(",")).from(TOKENIZER, Scanners.WHITESPACES.skipMany()), Scanners.isChar(']'))
            .map(Value::new);

    private static final Parser<Value> NULL_VALUE_PARSER = Scanners.stringCaseInsensitive("NULL").retn(Value.NULL_VALUE);
    private static final Parser<Value> BOOLEAN_PARSER = Parsers.or(Scanners.stringCaseInsensitive("FALSE").retn(false), Scanners.stringCaseInsensitive("TRUE").retn(true)).map(Value::new);
    private static final Parser<Value> VALUE_PARSER = Parsers.or(VALUE_SYNTACTIC_PARSER.from(VALUE_TOKENIZER, Scanners.WHITESPACES.skipMany()).map(Value::new), NULL_VALUE_PARSER, BOOLEAN_PARSER);
    private static final Parser<Value> VALUE_PARSER_WITH_IN = VALUE_PARSER.or(IN_VALUE_PARSER);

    static Parser<Value> get() {
        return VALUE_PARSER_WITH_IN;
    }
}
