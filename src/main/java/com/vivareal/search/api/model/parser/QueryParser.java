package com.vivareal.search.api.model.parser;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.*;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.jparsec.Parser.newReference;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.isChar;

@Component
public class QueryParser {

    private final Parser<QueryFragment> queryParser;
    private final Parser<QueryFragment> recursiveQueryParser;

    @Autowired
    public QueryParser(OperatorParser operatorParser, FilterParser filterParser, NotParser notParser) {
        queryParser = sequence(operatorParser.getLogicalOperatorParser().asOptional(), filterParser.get(), QueryFragmentItem::new);


        Parser.Reference<QueryFragment> ref = newReference();
        Parser<QueryFragment> lazy = ref.lazy();
        recursiveQueryParser = lazy.between(isChar('('), isChar(')'))
            .or(or(queryParser, operatorParser.getLogicalOperatorParser().map(QueryFragmentOperator::new), notParser.get().many().map(QueryFragmentNot::new)))
            .many()
            .label("query")
            .map(QueryFragmentList::new);
        ref.set(recursiveQueryParser);
    }

    @Trace
    public QueryFragment parse(String string) {
        return recursiveQueryParser.parse(string);
    }
}
