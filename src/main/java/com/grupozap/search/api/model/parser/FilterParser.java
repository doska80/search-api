package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.query.RelationalOperator.CONTAINS_ALL;
import static com.grupozap.search.api.model.query.RelationalOperator.DIFFERENT;
import static com.grupozap.search.api.model.query.RelationalOperator.EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.GREATER;
import static com.grupozap.search.api.model.query.RelationalOperator.GREATER_EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.IN;
import static com.grupozap.search.api.model.query.RelationalOperator.LESS;
import static com.grupozap.search.api.model.query.RelationalOperator.LESS_EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.LIKE;
import static com.grupozap.search.api.model.query.RelationalOperator.POLYGON;
import static com.grupozap.search.api.model.query.RelationalOperator.RADIUS;
import static com.grupozap.search.api.model.query.RelationalOperator.RANGE;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;

import com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.grupozap.search.api.model.query.Filter;
import com.grupozap.search.api.model.query.RelationalOperator;
import org.jparsec.Parser;

public class FilterParser {

  private final Parser<Filter> filterParser;

  public FilterParser(
      FieldParser fieldParser, OperatorParser operatorParser, ValueParser valueParser) {
    var normalParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(
                    DIFFERENT, EQUAL, GREATER_EQUAL, GREATER, IN, LESS_EQUAL, LESS, CONTAINS_ALL),
                valueParser.get(),
                Filter::new)
            .label("filter");
    var likeParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(LIKE),
                valueParser.getLikeValue(),
                Filter::new)
            .label("LIKE filter");
    var rangeParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(RANGE),
                valueParser.getRangeValue(),
                Filter::new)
            .label("RANGE filter");
    var viewportParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(RelationalOperator.VIEWPORT),
                valueParser.getGeoPointValue(Type.VIEWPORT),
                Filter::new)
            .label("VIEWPORT filter");
    var polygonParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(POLYGON),
                valueParser.getGeoPointValue(Type.POLYGON),
                Filter::new)
            .label("POLYGON filter");
    var radiusParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(RADIUS),
                valueParser.getGeoPointRadiusValue(),
                Filter::new)
            .label("RADIUS filter");

    filterParser =
        or(normalParser, rangeParser, likeParser, viewportParser, polygonParser, radiusParser);
  }

  Parser<Filter> get() {
    return filterParser;
  }
}
