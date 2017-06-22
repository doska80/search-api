package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.query.QueryFragmentItem;
import com.vivareal.search.api.model.query.QueryFragmentList;
import com.vivareal.search.api.model.query.QueryFragmentOperator;
import org.jparsec.Parser;
import org.jparsec.Parsers;

import static org.jparsec.Scanners.isChar;

public class QueryParser {
    public static final Parser<QueryFragment> QUERY_PARSER =
            Parsers.sequence(OperatorParser.LOGICAL_OPERATOR_PARSER.asOptional(), FilterParser.get(), QueryFragmentItem::new);

    public static final Parser<QueryFragment> RECURSIVE_QUERY_PARSER = getRecursive();

    private static Parser<QueryFragment> getRecursive() {
        Parser.Reference<QueryFragment> ref = Parser.newReference();
        Parser<QueryFragment> lazy = ref.lazy();
        Parser<QueryFragment> parser = lazy.between(isChar('('), isChar(')'))
                .or(Parsers.or(QUERY_PARSER, OperatorParser.LOGICAL_OPERATOR_PARSER.map(QueryFragmentOperator::new))).many()
                .map(QueryFragmentList::new);
        ref.set(parser);
        return parser;
    }

    public static Parser<QueryFragment> get() {
        return RECURSIVE_QUERY_PARSER;
    }
}
