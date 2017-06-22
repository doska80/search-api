package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

import static org.jparsec.Scanners.isChar;

public class QueryParser {
    public static final Parser<QueryFragment> QUERY_PARSER = Parsers.sequence(FilterParser.get(), LogicalOperatorParser.get().asOptional(), QueryFragment::new);

    public static final Parser<QueryFragment> RECURSIVE_QUERY_PARSER = getRecursive();

    private static Parser<QueryFragment> getRecursive() {
        Parser.Reference<QueryFragment> ref = Parser.newReference();
        Parser<QueryFragment> lazy = ref.lazy();
        Parser<QueryFragment> parser = lazy.between(isChar('('), isChar(')'))
                .or(QUERY_PARSER).many()
                .map(QueryFragment::new);
        ref.set(parser);
        return parser;
    }

    public static Parser<QueryFragment> get() {
        return RECURSIVE_QUERY_PARSER;
    }
}