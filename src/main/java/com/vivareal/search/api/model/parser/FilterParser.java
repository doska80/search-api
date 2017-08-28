package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Filter;
import org.jparsec.Parser;

import static com.vivareal.search.api.model.parser.OperatorParser.RELATIONAL_OPERATOR_PARSER;
import static com.vivareal.search.api.model.parser.OperatorParser.exact;
import static com.vivareal.search.api.model.query.RelationalOperator.LIKE;
import static com.vivareal.search.api.model.query.RelationalOperator.VIEWPORT;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;

public class FilterParser {

    private static final Parser<Filter> NORMAL_PARSER = sequence(FieldParser.get(), RELATIONAL_OPERATOR_PARSER, ValueParser.get(), Filter::new).label("filter");

    private static final Parser<Filter> VIEWPORT_PARSER = sequence(FieldParser.get(), exact(VIEWPORT), ValueParser.Viewport.get(), Filter::new).label("viewport filter");

    private static final Parser<Filter> LIKE_PARSER = sequence(FieldParser.get(), exact(LIKE), ValueParser.Like.get(), Filter::new).label("LIKE filter");

    private static final Parser<Filter> FILTER_PARSER = or(VIEWPORT_PARSER, LIKE_PARSER, NORMAL_PARSER);

    static Parser<Filter> get() {
        return FILTER_PARSER;
    }
}
