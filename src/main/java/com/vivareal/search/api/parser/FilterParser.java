package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

import java.util.List;

public class FilterParser {

    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FieldParser.get(), ComparisonParser.get(), ValueParser.get()).map(Filter::new).cast();
            //sequence(FieldParser.get(), ComparisonParser.get(), ValueParser.get()).many().map((List<String> field) -> new Filter(field)).cast();

    public static Parser<Filter> get() {
        return SINGLE_EXPRESSION_PARSER;
    }

}
