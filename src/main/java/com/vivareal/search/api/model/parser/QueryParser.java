package com.vivareal.search.api.model.parser;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.*;
import org.jparsec.Parser;

import static com.vivareal.search.api.model.parser.OperatorParser.LOGICAL_OPERATOR_PARSER;
import static org.jparsec.Parser.newReference;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.isChar;

public class QueryParser {
    static final Parser<QueryFragment> QUERY_PARSER =
            sequence(LOGICAL_OPERATOR_PARSER.asOptional(), FilterParser.get(), QueryFragmentItem::new);

    static final Parser<QueryFragment> RECURSIVE_QUERY_PARSER = getRecursive();

    private static Parser<QueryFragment> getRecursive() {
        Parser.Reference<QueryFragment> ref = newReference();
        Parser<QueryFragment> lazy = ref.lazy();
        Parser<QueryFragment> parser = lazy.between(isChar('('), isChar(')'))
                .or(or(QUERY_PARSER, OperatorParser.LOGICAL_OPERATOR_PARSER.map(QueryFragmentOperator::new), NotParser.get().many().map(QueryFragmentNot::new)))
                .many()
                .label("query")
                .map(QueryFragmentList::new);
        ref.set(parser);
        return parser;
    }

    @Trace
    public static QueryFragment parse(String string) {
        return RECURSIVE_QUERY_PARSER.parse(string);
    }
}
