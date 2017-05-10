package com.vivareal.search.api.parser;


import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

import java.util.List;

import static com.vivareal.search.api.parser.RelationalOperatorParser.getToken;

public class ValueParser {

    public static final Parser<Value> SIMPLE_VALUE_PARSER = Parsers.or(
            Scanners.DECIMAL,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
            Scanners.IDENTIFIER)
        .map(Value::new);
//        .cast();

    public static final Parser<List<Value>> MULTI_VALUE_PARSER = Parsers.between(Scanners.isChar('['), SIMPLE_VALUE_PARSER.sepBy(Scanners.isChar(',')), Scanners.isChar(']'));
//            RelationalOperatorParser.getToken("IN").followedBy(
//            Parsers.between(RelationalOperatorParser.getToken("["), SIMPLE_VALUE_PARSER, RelationalOperatorParser.getToken("]"))
//    );

    public static final Parser<Value> VALUE_PARSER = Parsers.or(
            Scanners.DECIMAL,
            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
            Scanners.IDENTIFIER)
        .map(Value::new);

    public static Parser<Value> getSimple() {
        return SIMPLE_VALUE_PARSER;
    }

    public static Parser<?> get() {
        return MULTI_VALUE_PARSER;
    }

}
