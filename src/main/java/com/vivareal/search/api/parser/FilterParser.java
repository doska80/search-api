package com.vivareal.search.api.parser;

import org.jparsec.OperatorTable;
import org.jparsec.Parser;
import org.jparsec.Parsers;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.jparsec.Scanners.isChar;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();
    private static final Parser<RelationalOperator> RELATIONAL_OP_PARSER = RelationalOperatorParser.get();
    private static final Parser<Value> VALUE_PARSER = ValueParser.get();

    private static final UnaryOperator<Filter> AND = f -> f.setLogicalOperator(LogicalOperator.AND);
    private static final UnaryOperator<Filter> NOT = f -> f.setLogicalOperator(LogicalOperator.NOT);
    private static final UnaryOperator<Filter> OR = f -> f.setLogicalOperator(LogicalOperator.OR);

    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2])
    ).cast();

    private static final Parser<Filter> MULTI_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2]) // TODO consider all LogicalOperators and recursiveness ("(" and ")")
    ).cast();

    public static Parser<Filter> getOne() {
        return SINGLE_EXPRESSION_PARSER;
    }

    public static Parser<List<Filter>> getList() {
        return SINGLE_EXPRESSION_PARSER.many();
    }

    /**
     * Source of inspiration: https://github.com/jparsec/jparsec/blob/master/jparsec-examples/src/main/java/org/jparsec/examples/calculator/Calculator.java
     * @return
     */
    public static Parser<Filter> get() {
        Parser.Reference<Filter> ref = Parser.newReference();
        Parser<Filter> term = ref.lazy().between(isChar('('), isChar(')')).or(MULTI_EXPRESSION_PARSER);
        Parser<Filter> parser = new OperatorTable<Filter>()
                .prefix(op("NOT", NOT), 100)
//                .infixl(op("&&", AND), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
//                .infixl(op("AND", AND), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
//                .infixl(op("OR", OR), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
                .build(term);
        ref.set(term);
        return parser;
    }

    private static <T> Parser<T> op(String operator, T value) {
//        among(operator).retn(value); // FIXME how should I do that? :(
        return isChar('x').retn(value); // FIXME wrong, wrong, WRONG! should check for the entire "operator" string
    }

}
