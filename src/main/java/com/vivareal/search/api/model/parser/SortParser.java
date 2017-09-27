package com.vivareal.search.api.model.parser;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;

import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;

public class SortParser {

    private static final Parser<Sort> SORT_PARSER = sequence(FieldParser.getWithoutNot(), OperatorParser.ORDER_OPERATOR_PARSER.optional(OrderOperator.ASC), Sort::new).sepBy(isChar(',').next(WHITESPACES.skipMany())).label("sort").map(Sort::new);

    @Trace
    public static Sort parse(String string) {
        return SORT_PARSER.parse(string);
    }
}
