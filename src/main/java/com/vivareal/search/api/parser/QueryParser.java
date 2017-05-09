package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QueryParser {

    private static final Parser<Filter> FILTER_PARSER = FilterParser.get();
    private static final Parser<LogicalOperator> LOGICAL_OPERATOR_PARSER = LogicalOperatorParser.get();
    private static final Parser<List<Optional<Object>>> NEXT_SINGLE_EXPRESSION_PARSER = Parsers.array(LOGICAL_OPERATOR_PARSER, FILTER_PARSER).cast().asOptional().many();
    private static final Parser<List<QueryFragment>> MULTI_EXPRESSION_PARSER = Parsers.array(FILTER_PARSER, NEXT_SINGLE_EXPRESSION_PARSER).map(objects -> {
        List<QueryFragment> queryFragments = new ArrayList<>();
        queryFragments.add(new QueryFragment((Filter) objects[0]));
        for (Optional<Object> expression : (ArrayList<Optional<Object>>) objects[1]) {
            Object[] expressionList = (Object[]) expression.orElse(new Object[0]);
            if (expressionList.length == 0)
                break;
            queryFragments.add(new QueryFragment((LogicalOperator) expressionList[0]));
            queryFragments.add(new QueryFragment((Filter) expressionList[1]));
        }
        return queryFragments;
    }).cast();

//
//    private static final UnaryOperator<QueryFragment> AND = e -> e.setRelationalOperator(LogicalOperator.AND);
//    private static final UnaryOperator<QueryFragment> NOT = e -> e.setRelationalOperator(LogicalOperator.NOT);
//    private static final UnaryOperator<QueryFragment> OR = e -> e.setRelationalOperator(LogicalOperator.OR);


    public static Parser<List<QueryFragment>> get() {
        return MULTI_EXPRESSION_PARSER;
    }


//    public static void main(String[] args) {
//        List<QueryFragment> foi = FilterParser.getMulti().parse("title=lalla AND (bathrooms=10 OR mamud=viadim) AND garages=123");
////        List<QueryFragment> foi = FilterParser.getMulti().parse("title=lalla");
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
