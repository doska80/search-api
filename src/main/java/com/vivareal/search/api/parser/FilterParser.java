package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

public class FilterParser {

    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FieldParser.get(), ComparisonParser.get(), ValueParser.get()).map(Filter::new).cast();

    public static Parser<Filter> get() {
        return SINGLE_EXPRESSION_PARSER;
    }

}
