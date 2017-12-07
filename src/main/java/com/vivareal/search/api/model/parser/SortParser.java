package com.vivareal.search.api.model.parser;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.jparsec.Parsers.between;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.*;

@Component
public class SortParser {

    static final String SORT_FILTER_FIELD = "sortFilter";

    private final Parser<Sort> sortParser;

    @Autowired
    public SortParser(FieldParser fieldParser, OperatorParser operatorParser, QueryParser queryParser) {
        Parser<OrderOperator> orderOperatorParser = operatorParser.getOrderOperatorParser().optional(OrderOperator.ASC).label("sortOperator");
        Parser<Optional<QueryFragment>> sortFilterParser = sequence(string(SORT_FILTER_FIELD), between(WHITESPACES.skipMany(), string(":"), WHITESPACES.skipMany()), queryParser.getRecursiveQueryParser()).asOptional();

        sortParser = sequence(fieldParser.getWithoutNot(), orderOperatorParser, sortFilterParser, Sort::new)
            .sepBy(isChar(',')
            .next(WHITESPACES.skipMany()))
            .label("sort")
            .map(Sort::new);
    }

    @Trace
    public Sort parse(String string) {
        return sortParser.parse(string);
    }
}
