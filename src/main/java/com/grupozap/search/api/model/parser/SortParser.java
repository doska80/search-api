package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.SINGLE;
import static com.grupozap.search.api.model.query.OrderOperator.ASC;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.jparsec.Parsers.*;
import static org.jparsec.Scanners.*;

import com.grupozap.search.api.model.query.GeoPointValue;
import com.grupozap.search.api.model.query.OrderOperator;
import com.grupozap.search.api.model.query.QueryFragment;
import com.grupozap.search.api.model.query.Sort;
import com.newrelic.api.agent.Trace;
import java.util.Optional;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SortParser {

  private static final String SORT_FILTER_FIELD = "sortFilter";

  private final Parser<Sort> sortSingleParser;
  private final Parser<Sort> sortParser;

  @Autowired
  public SortParser(
      FieldParser fieldParser,
      OperatorParser operatorParser,
      ValueParser valueParser,
      QueryParser queryParser) {

    // SortFilter used in any kind of sort
    Parser<Optional<QueryFragment>> sortFilterParser = createSortFilterParser(queryParser);

    // Sort Optional Settings: ASC/DESC || NEAR [GeoPointValue]
    Parser<SortOptionalSettings> sortOptionalSettingsParser =
        createSortOptionalSettingsParser(operatorParser, valueParser);

    // Single parser to sort facets
    sortSingleParser =
        sequence(
            fieldParser.getWithNot(),
            operatorParser.getOrderOperatorParser().optional(ASC).label("sortOperator"),
            sortFilterParser,
            Sort::new);

    sortParser =
        sequence(
                fieldParser.getWithNot(),
                sortOptionalSettingsParser.asOptional(),
                sortFilterParser,
                (field, sortOptionalSettings, queryFragment) ->
                    new Sort(
                        field,
                        sortOptionalSettings
                            .map(SortOptionalSettings::getOrderOperator)
                            .orElse(ASC),
                        sortOptionalSettings.flatMap(SortOptionalSettings::getGeoPointValue),
                        queryFragment))
            .sepBy(isChar(',').next(WHITESPACES.skipMany()))
            .label("sort")
            .map(Sort::new);
  }

  private Parser<Optional<QueryFragment>> createSortFilterParser(QueryParser queryParser) {
    return sequence(
            string(SORT_FILTER_FIELD),
            between(WHITESPACES.skipMany(), string(":"), WHITESPACES.skipMany()),
            queryParser.getRecursiveQueryParser())
        .asOptional();
  }

  private Parser<SortOptionalSettings> createSortOptionalSettingsParser(
      OperatorParser operatorParser, ValueParser valueParser) {
    return or(
        operatorParser.getOrderOperatorParser().map(SortOptionalSettings::new),
        sequence(
            between(WHITESPACES.skipMany(), string("NEAR").retn(ASC), WHITESPACES.skipMany()),
            between(
                WHITESPACES.skipMany(),
                valueParser.getGeoPointValue(SINGLE),
                WHITESPACES.skipMany()),
            SortOptionalSettings::new));
  }

  private static class SortOptionalSettings {
    final OrderOperator orderOperator;
    final Optional<GeoPointValue> geoPointValue;

    private SortOptionalSettings(OrderOperator orderOperator) {
      this.orderOperator = orderOperator;
      this.geoPointValue = empty();
    }

    private SortOptionalSettings(OrderOperator orderOperator, GeoPointValue geoPointValue) {
      this.orderOperator = orderOperator;
      this.geoPointValue = of(geoPointValue);
    }

    private OrderOperator getOrderOperator() {
      return orderOperator;
    }

    private Optional<GeoPointValue> getGeoPointValue() {
      return geoPointValue;
    }
  }

  public Parser<Sort> getSingleSortParser() {
    return sortSingleParser;
  }

  @Trace
  public Sort parse(String string) {
    return sortParser.parse(string);
  }
}
