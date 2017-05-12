package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();
    private static final Parser<RelationalOperator> RELATIONAL_OP_PARSER = RelationalOperatorParser.get();
    private static final Parser<Value> VALUE_PARSER = ValueParser.get();
    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, VALUE_PARSER).map(Filter::new).cast();

    static Parser<Filter> get() {
        return SINGLE_EXPRESSION_PARSER;
    }

}
