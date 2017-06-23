package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Filter;
import org.jparsec.Parser;
import org.jparsec.Parsers;

public class FilterParser {
    private static final Parser<Filter> FILTER_PARSER = Parsers.sequence(NotParser.get(), FieldParser.get(), OperatorParser.RELATIONAL_OPERATOR_PARSER, ValueParser.get(), Filter::new);

    static Parser<Filter> get() {
        return FILTER_PARSER;
    }
}
