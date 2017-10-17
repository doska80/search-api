package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.vivareal.search.api.model.query.Filter;
import org.jparsec.Parser;

import static com.vivareal.search.api.model.parser.OperatorParser.RELATIONAL_OPERATOR_PARSER;
import static com.vivareal.search.api.model.parser.OperatorParser.exact;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;

public class FilterParser {

    private static final Parser<Filter> NORMAL_PARSER = sequence(FieldParser.get(), exact(DIFFERENT, EQUAL, GREATER_EQUAL, GREATER, IN, LESS_EQUAL, LESS), ValueParser.get(), Filter::new).label("filter");

    private static final Parser<Filter> LIKE_PARSER = sequence(FieldParser.get(), exact(LIKE), ValueParser.Like.get(), Filter::new).label("LIKE filter");

    private static final Parser<Filter> RANGE_PARSER = sequence(FieldParser.get(), exact(RANGE), ValueParser.Range.get(), Filter::new).label("RANGE filter");

    private static final Parser<Filter> VIEWPORT_PARSER = sequence(FieldParser.get(), exact(VIEWPORT), ValueParser.GeoPoint.get(Type.VIEWPORT), Filter::new).label("VIEWPORT filter");

    private static final Parser<Filter> POLYGON_PARSER = sequence(FieldParser.get(), exact(POLYGON), ValueParser.GeoPoint.get(Type.POLYGON), Filter::new).label("POLYGON filter");

    private static final Parser<Filter> FILTER_PARSER = or(RANGE_PARSER, VIEWPORT_PARSER, POLYGON_PARSER, LIKE_PARSER, NORMAL_PARSER);

    static Parser<Filter> get() {
        return FILTER_PARSER;
    }
}
