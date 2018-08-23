package com.grupozap.search.api.model.parser;

import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;

import com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.vivareal.search.api.model.query.Filter;
import org.jparsec.Parser;

public class FilterParser {

  private final Parser<Filter> filterParser;

  public FilterParser(
      FieldParser fieldParser, OperatorParser operatorParser, ValueParser valueParser) {
    Parser<Filter> normalParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(
                    DIFFERENT, EQUAL, GREATER_EQUAL, GREATER, IN, LESS_EQUAL, LESS, CONTAINS_ALL),
                valueParser.get(),
                Filter::new)
            .label("filter");
    Parser<Filter> likeParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(LIKE),
                valueParser.getLikeValue(),
                Filter::new)
            .label("LIKE filter");
    Parser<Filter> rangeParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(RANGE),
                valueParser.getRangeValue(),
                Filter::new)
            .label("RANGE filter");
    Parser<Filter> viewportParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(VIEWPORT),
                valueParser.getGeoPointValue(Type.VIEWPORT),
                Filter::new)
            .label("VIEWPORT filter");
    Parser<Filter> polygonParser =
        sequence(
                fieldParser.get(),
                operatorParser.exact(POLYGON),
                valueParser.getGeoPointValue(Type.POLYGON),
                Filter::new)
            .label("POLYGON filter");

    filterParser = or(normalParser, rangeParser, likeParser, viewportParser, polygonParser);
  }

  Parser<Filter> get() {
    return filterParser;
  }
}
