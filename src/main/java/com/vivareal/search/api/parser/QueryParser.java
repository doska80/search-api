package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

import static org.jparsec.Scanners.isChar;

public class QueryParser {

    private static final Parser<Filter> FILTER_PARSER;
    static {
        System.out.println("FILTER_PARSER");
        FILTER_PARSER = FilterParser.get();
    }
    private static final Parser<LogicalOperator> LOGICAL_OPERATOR_PARSER;
    static {
        System.out.println("LOGICAL_OPERATOR_PARSER");
        LOGICAL_OPERATOR_PARSER = LogicalOperatorParser.get();
    }

    static private final Parser<QueryFragment> QUERY_PARSER;
    static {
        System.out.println("QUERY_PARSER");
        QUERY_PARSER = Parsers.sequence(FILTER_PARSER, LOGICAL_OPERATOR_PARSER.asOptional(), QueryFragment::new);
    }

    static private final Parser<Boolean> NOT_PARSER2;
    static {
        System.out.println("NOT_PARSER2");
        NOT_PARSER2 = Scanners.WHITESPACES.skipMany().next(Scanners.string("NOT2").next(Scanners.WHITESPACES.atLeast(1))).succeeds();
    }

//    static private final Parser<QueryFragment> RECURSIVE_PARSER;
//    static {
//        System.out.println("RECURSIVE_PARSER");
//        RECURSIVE_PARSER = Parsers.sequence(NOT_PARSER2.asOptional(), getRecursive(QUERY_PARSER), QueryFragment::new);
//    }
    private static Parser<QueryFragment> getRecursive(Parser<QueryFragment> p) {
        System.out.println("getRecursive()");
        Parser.Reference<QueryFragment> ref = Parser.newReference();
        Parser<QueryFragment> lazy = ref.lazy();
        Parser<QueryFragment> parser = lazy.between(isChar('('), isChar(')'))
                .or(p).many()
                .map(QueryFragment::new);
        ref.set(parser);
        return parser;
    }

    public static Parser<QueryFragment> get() {
        return getRecursive(QUERY_PARSER);
    }
}