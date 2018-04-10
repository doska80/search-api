package com.vivareal.search.api.model.parser;

import static com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type.SINGLE;
import static org.jparsec.Parsers.between;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.*;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.*;
import java.util.Optional;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SortParser {

  private static final String SORT_FILTER_FIELD = "sortFilter";

  private final Parser<Sort> sortSingleParser;
  private final Parser<Sort> sortNearParser;
  private final Parser<Sort> sortParser;

  @Autowired
  public SortParser(
      FieldParser fieldParser,
      OperatorParser operatorParser,
      ValueParser valueParser,
      QueryParser queryParser) {
    Parser<OrderOperator> orderOperatorParser =
        operatorParser.getOrderOperatorParser().optional(OrderOperator.ASC).label("sortOperator");

    Parser<Optional<QueryFragment>> sortFilterParser =
        sequence(
                string(SORT_FILTER_FIELD),
                between(WHITESPACES.skipMany(), string(":"), WHITESPACES.skipMany()),
                queryParser.getRecursiveQueryParser())
            .asOptional();

    sortSingleParser =
        sequence(fieldParser.getWithoutNot(), orderOperatorParser, sortFilterParser, Sort::new);

    sortNearParser =
        sequence(
            fieldParser.getWithoutNot(),
            between(WHITESPACES.skipMany(), string("NEAR"), WHITESPACES.skipMany()),
            between(
                WHITESPACES.skipMany(),
                valueParser.getGeoPointValue(SINGLE),
                WHITESPACES.skipMany()),
            sortFilterParser,
            (field, voidNear, geoPointValue, queryFragment) ->
                new Sort(field, geoPointValue, queryFragment));
    sortParser =
        Parsers.or(sortNearParser, sortSingleParser)
            .sepBy(isChar(',').next(WHITESPACES.skipMany()))
            .label("sort")
            .map(Sort::new);
  }

  public Parser<Sort> getSingleSortParser() {
    return sortSingleParser;
  }

  @Trace
  public Sort parse(String string) {
    return sortParser.parse(string);
  }
}
