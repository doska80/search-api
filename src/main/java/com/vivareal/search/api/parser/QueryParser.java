package com.vivareal.search.api.parser;

import org.jparsec.OperatorTable;
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

    public static Parser<List<QueryFragment>> get() {
        return MULTI_EXPRESSION_PARSER;
    }

    public static Parser<List<QueryFragment>> getRecursive() {
        Parser.Reference<List<QueryFragment>> ref = Parser.newReference();
        Parser<List<QueryFragment>> unit = ref.lazy().between(LogicalOperatorParser.getToken("("), LogicalOperatorParser.getToken(")")).or(MULTI_EXPRESSION_PARSER);
        Parser<List<QueryFragment>> parser = new OperatorTable<List<QueryFragment>>().build(unit); // TODO understand WHY we need this :/
        ref.set(parser);
        return parser;
    }

}
