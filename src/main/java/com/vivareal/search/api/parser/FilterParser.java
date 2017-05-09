package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();
    private static final Parser<RelationalOperator> RELATIONAL_OP_PARSER = RelationalOperatorParser.get();
    private static final Parser<Value> VALUE_PARSER = ValueParser.get();
    private static final Parser<LogicalOperator> LOGICAL_OPERATOR_PARSER = LogicalOperatorParser.get();
//
//    private static final UnaryOperator<Expression> AND = e -> e.setRelationalOperator(LogicalOperator.AND);
//    private static final UnaryOperator<Expression> NOT = e -> e.setRelationalOperator(LogicalOperator.NOT);
//    private static final UnaryOperator<Expression> OR = e -> e.setRelationalOperator(LogicalOperator.OR);

    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2]) // FIXME that's ugly. Fix me.
    ).cast();

    private static final Parser<Filter> NEXT_SINGLE_EXPRESSION_PARSER = Parsers.array(LOGICAL_OPERATOR_PARSER, SINGLE_EXPRESSION_PARSER).cast();

    private static final Parser<List<Expression>> MULTI_EXPRESSION_PARSER = Parsers.array(SINGLE_EXPRESSION_PARSER, NEXT_SINGLE_EXPRESSION_PARSER.asOptional().many()).map(objects -> {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(new Expression((Filter) objects[0]));
        for (Optional<Object> expression : (ArrayList<Optional<Object>>) objects[1]) {
            Object[] expressionList = (Object[]) expression.orElse(new Object[0]);
            if (expressionList.length == 0)
                break;
            expressions.add(new Expression((LogicalOperator) expressionList[0]));
            expressions.add(new Expression((Filter) expressionList[1]));
        }
        return expressions;
    }).cast();

    public static Parser<Filter> getOne() {
        return SINGLE_EXPRESSION_PARSER;
    }

    public static Parser<List<Expression>> getMulti() {
        return MULTI_EXPRESSION_PARSER;
    }

    public static Parser<List<Filter>> getList() {
        return SINGLE_EXPRESSION_PARSER.many();
    }

//    public static void main(String[] args) {
//        List<Expression> foi = FilterParser.getMulti().parse("title=lalla AND (bathrooms=10 OR mamud=viadim) AND garages=123");
////        List<Expression> foi = FilterParser.getMulti().parse("title=lalla");
//
//        foi.forEach(expression -> {
//            System.out.println(expression);
//        });
//
//    }


//    static {
//        Parser.Reference<Filter> ref = Parser.newReference();
//        MULTI_EXPRESSION_PARSER = ref.lazy().between(isChar('('), isChar(')')).or(SINGLE_EXPRESSION_PARSER);
//        ref.set(MULTI_EXPRESSION_PARSER);
//    }

//    /**
//     * Source of inspiration: https://github.com/jparsec/jparsec/blob/master/jparsec-examples/src/main/java/org/jparsec/examples/calculator/Calculator.java
//     * @return
//     */
//    public static Parser<Filter> get() {
//        Parser.Reference<Filter> ref = Parser.newReference();
//        Parser<Filter> term = ref.lazy().between(isChar('('), isChar(')')).or(MULTI_EXPRESSION_PARSER);
//        Parser<?> parser = new OperatorTable<Filter>()
//                .prefix(op("NOT", NOT), 100)
//                .infixl(op("&&", AND), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
////                .infixl(op("AND", AND), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
////                .infixl(op("OR", OR), 10) // FIXME wrong, expecting BinaryOperator and not UnaryOperator
//                .build(term);
//        ref.set(term);
//        return parser;
//    }
//
//    private static <T> Parser<T> op(String operator, T value) {
////        among(operator).retn(value); // FIXME how should I do that? :(
//        return isChar('x').retn(value); // FIXME wrong, wrong, WRONG! should check for the entire "operator" string
//    }

}
