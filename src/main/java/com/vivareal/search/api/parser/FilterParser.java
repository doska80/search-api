package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

public class FilterParser {
    private static final Parser<Filter> FILTER_PARSER = Parsers.sequence(NotParser.get(), FieldParser.get(), OperatorParser.RELATIONAL_OPERATOR_PARSER, ValueParser.get(), Filter::new);

    static Parser<Filter> get() {
        return FILTER_PARSER;
    }
}