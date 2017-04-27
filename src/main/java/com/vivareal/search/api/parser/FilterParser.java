package com.vivareal.search.api.parser;

import org.jparsec.*;
import org.jparsec.functors.Map;
import org.jparsec.functors.Pair;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static org.jparsec.Scanners.isChar;

public class FilterParser {

//    static final Terminals OPERATORS = Terminals.operators(":", "EQ", "NEQ", "GT", "LT", "GTE", "LTE", "OR", "AND", "NOT", "(", ")", "IN", "[", "]", ",", "<>");
//    static final Terminals OPERATORS = Terminals.operators("=", "OR", "AND", "NOT", "(", ")", "IN", "[", "]", ",", "<>");
//    static final Parser<String> FIELD_NAME_TOKENIZER = Terminals.Identifier.TOKENIZER.source();
    static final Parser<?> QUOTED_STRING_TOKENIZER = Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER.or(Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER);
    static final Terminals TERMINALS = Terminals.operators(new String[] { ":", "=", "EQ", "NEQ", "GT", "LT", "GTE", "LTE", "(", ")", "[", "]", ",", "<>" }).words(Scanners.IDENTIFIER).keywords(new String[] { "OR", "AND", "NOT", "IN" }).build();

    static final Parser<?> TOKENIZER = Parsers.or(TERMINALS.tokenizer(), QUOTED_STRING_TOKENIZER);

    private static Parser<FieldNode> fieldNodeParser = Parsers.sequence(Terminals.fragment(Tokens.Tag.IDENTIFIER).map(new Map<String, FieldNode>() {
        @Override
        public FieldNode map(String from) {
            String fragment = from;
            return new FieldNode(fragment);
        }
    })).cast();

    public static Parser<FieldNode> parser = fieldNodeParser.from(TOKENIZER, Scanners.WHITESPACES);

//    static final BinaryOperator<Kct> EQUAL = (a, b) -> new Kct(a, b);
//
//    static final Parser<String> kctParser = new OperatorTable<String>()
//            .infixl(isChar('=').retn(EQUAL), 10)
//            .build(TOKENIZER.cast());

    private static class Kct {
        private final Object a;
        private final Object b;

        private Kct(String a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "Kct{" +
                    "a=" + a +
                    ", b=" + b +
                    '}';
        }
    }

    private static class FieldNode {
        final String text;

        public FieldNode(String text) {

            this.text = text;
        }
    }

    public static void main(String[] args) {
//        String FUCKING_TEST = "bla EQ 'ble AND bli' AND foo='abc' AND bar<>'def' OR (biz IN ['a', 'b', 'c'] AND NOT baz = 'foo')";
        String SMALL_FUCKING_TEST = "\"bla\"=\"bleeeeeeeee\"";
//        Kct response =
//        System.out.println(kctParser.parse(SMALL_FUCKING_TEST));
//        System.out.println(response);
//        List<?> tokenizerResult = Parsers.or(TOKENIZER, Scanners.WHITESPACES.cast()).many().parse(FUCKING_TEST);
//        tokenizerResult.forEach(x -> {
//            if (x == null) {
//                System.out.println("null!");
//            } else {
//                System.out.println(x.getClass());
//                System.out.println(x);
//            }
//            System.out.println("-=-=-=-=-=\n");
//        });
////        System.out.println(tokenizerResult.toString());
//
//
//        FieldNode parseResult = parser.parse("field=value");
//        System.out.println(parseResult.text);

    }



}
