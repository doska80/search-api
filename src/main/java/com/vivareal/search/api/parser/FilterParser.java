package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;

public class FilterParser {

    private static final Parser<Field> FIELD_PARSER = FieldParser.get();

    private static final Parser<?> IN_OP_PARSER = RelationalOperatorParser.getIn();
    private static final Parser<RelationalOperator> RELATIONAL_OP_PARSER = RelationalOperatorParser.get();

    private static final Parser<Filter> MULTI_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, IN_OP_PARSER, ValueParser.getMulti()).map(Filter::new).cast(); // IN
    private static final Parser<Filter> SINGLE_EXPRESSION_PARSER = Parsers.array(FIELD_PARSER, RELATIONAL_OP_PARSER, ValueParser.getSingle()).map(Filter::new).cast(); // EQ, LT, GT....

    private static final Parser<Filter> EXPRESSION_PARSER = Parsers.or(SINGLE_EXPRESSION_PARSER, MULTI_EXPRESSION_PARSER);

    static Parser<Filter> get() {
        return EXPRESSION_PARSER;
    }

}
