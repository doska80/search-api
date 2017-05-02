package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

import java.util.List;

import static org.jparsec.Scanners.isChar;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();
    private static final Parser<RelationalOperator> RELATIONAL_OP_PARSER = RelationalOperatorParser.get();
    private static final Parser<Value> VALUE_PARSER = ValueParser.get();

    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2])
    ).cast();

    private static final Parser<Filter> MULTI_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2])
    ).cast();

    public static Parser<Filter> getOne() {
        return SINGLE_EXPRESSION_PARSER;
    }

    public static Parser<List<Filter>> getList() {
        return SINGLE_EXPRESSION_PARSER.many();
    }

    public static Parser<Filter> get() {
        Parser.Reference<Filter> ref = Parser.newReference();
        Parser<Filter> term = ref.lazy().between(isChar('('), isChar(')')).or(MULTI_EXPRESSION_PARSER);

//        Parser<Integer> parser = new OperatorTable<Integer>()
//                .prefix(isChar('A').retn(AND), 100)
//                .infixl(op('+', PLUS), 10)
//                .infixl(op('-', MINUS), 10)
//                .infixl(op('*', MUL), 20)
//                .infixl(op('/', DIV), 20)
//                .infixl(op('%', MOD), 20)
//                .build(term);
//        ref.set(term);
//        return parser;
        return null;
    }

}
