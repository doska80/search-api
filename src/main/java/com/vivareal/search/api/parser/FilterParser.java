package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();
    private static final Parser<Comparison> COMPARISON_PARSER = ComparisonParser.get();
    private static final Parser<Value> VALUE_PARSER = ValueParser.get();
    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, COMPARISON_PARSER, VALUE_PARSER).map((Object[] expression) ->
            new Filter((Field) expression[0], (Comparison) expression[1], (Value) expression[2])
    ).cast();

    public static Parser<Filter> get() {
        return SINGLE_EXPRESSION_PARSER;
    }

}
