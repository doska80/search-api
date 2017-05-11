package com.vivareal.search.api.parser;


import org.jparsec.*;

import java.util.List;

public class ValueParser {

//    public static final Terminals OPERATORS = Terminals.operators("[", "]", ",");
//    public static final Parser<Void> IGNORED = Scanners.WHITESPACES.skipMany();
//
//    public static final Parser<?> SCANNER_VALUE_PARSER = Parsers.or(
//            Scanners.DECIMAL,
//            Scanners.IDENTIFIER,
//            Scanners.SINGLE_QUOTE_STRING,
//            Scanners.DOUBLE_QUOTE_STRING
//    );
//
//    public static final Parser<?> TERMINAL_VALUE_PARSER = Parsers.or(
//            Terminals.DecimalLiteral.TOKENIZER,
//            Terminals.Identifier.TOKENIZER,
//            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
//            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
//    );
//
//    public static final Parser<?> SIMPLE_VALUE_PARSER = Parsers.or(
////            SCANNER_VALUE_PARSER//,
//            TERMINAL_VALUE_PARSER
//    );
//
//    public Object x = Terminals.DecimalLiteral.TOKENIZER;
//
//    public static final Parser<?> VALUE_TOKENIZER = Parsers.or(OPERATORS.tokenizer().cast(), TERMINAL_VALUE_PARSER);
//
//    public static final Parser<List<Value>> MULTI_VALUE_PARSER = Parsers.between(
//            OPERATORS.token("["),
//            SIMPLE_VALUE_PARSER.sepBy(OPERATORS.token(",")),
//            OPERATORS.token("]")
//    ).from(VALUE_TOKENIZER, IGNORED).cast();
//
//
////            .from(VALUE_TOKENIZER, Scanners.WHITESPACES.skipMany()).cast();
//
////            RelationalOperatorParser.getToken("IN").followedBy(
////            Parsers.between(RelationalOperatorParser.getToken("["), SIMPLE_VALUE_PARSER, RelationalOperatorParser.getToken("]"))
////    );
//
//    public static final Parser<Value> VALUE_PARSER = Parsers.or(
//            Scanners.DECIMAL,
//            Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
//            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
//            Scanners.IDENTIFIER
//    ).map(Value::new);

    public static Parser<Value> getSimple() {
//        return SIMPLE_VALUE_PARSER.cast();
//        return MULTI_VALUE_PARSER;
        Terminals operators = Terminals.operators(","); // only one operator supported so far
        Parser<?> valueTokenizer = Parsers.or(
                Terminals.DecimalLiteral.TOKENIZER,
//                Terminals.IntegerLiteral.TOKENIZER,
                Terminals.Identifier.TOKENIZER,
                Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
                Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
        );
        Parser<String> valueSyntacticParser = Parsers.or(
                Terminals.DecimalLiteral.PARSER,
//                Terminals.IntegerLiteral.PARSER,
                Terminals.StringLiteral.PARSER
        );
        Parser<?> tokenizer = Parsers.or(operators.tokenizer(), valueTokenizer); // tokenizes the operators and integer
//        Parser<List<String>> integers =  Parsers.between(
//                Scanners.isChar('['),
//                valueSyntacticParser.from(tokenizer, Scanners.WHITESPACES.skipMany());
//                Scanners.isChar(']')
//        );
        return valueSyntacticParser.from(valueTokenizer, Scanners.WHITESPACES.skipMany()).map(Value::new);
    }

    public static Parser<List<Value>> get() {
//        return MULTI_VALUE_PARSER;
        Terminals operators = Terminals.operators(","); // only one operator supported so far
        Parser<?> valueTokenizer = Parsers.or(
                Terminals.DecimalLiteral.TOKENIZER,
//                Terminals.IntegerLiteral.TOKENIZER,
                Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
                Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
        );
        Parser<String> valueSyntacticParser = Parsers.or(
                Terminals.DecimalLiteral.PARSER,
//                Terminals.IntegerLiteral.PARSER,
                Terminals.StringLiteral.PARSER
        );
        Parser<?> tokenizer = Parsers.or(operators.tokenizer(), valueTokenizer); // tokenizes the operators and integer
        Parser<List<String>> integers =  Parsers.between(
                Scanners.isChar('['),
                valueSyntacticParser.sepBy(operators.token(",")).from(tokenizer, Scanners.WHITESPACES.skipMany()),
                Scanners.isChar(']')
        );
        return integers.cast();
    }

}
