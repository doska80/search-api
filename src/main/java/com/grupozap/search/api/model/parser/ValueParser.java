package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.SINGLE;
import static com.grupozap.search.api.model.query.RelationalOperator.RADIUS;
import static org.jparsec.Parsers.between;
import static org.jparsec.Parsers.longer;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.DECIMAL;
import static org.jparsec.Scanners.DOUBLE_QUOTE_STRING;
import static org.jparsec.Scanners.INTEGER;
import static org.jparsec.Scanners.SINGLE_QUOTE_STRING;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;
import static org.jparsec.Scanners.string;
import static org.jparsec.Scanners.stringCaseInsensitive;

import com.grupozap.search.api.model.query.GeoPointRadiusValue;
import com.grupozap.search.api.model.query.GeoPointValue;
import com.grupozap.search.api.model.query.LikeValue;
import com.grupozap.search.api.model.query.RangeValue;
import com.grupozap.search.api.model.query.Value;
import java.util.Optional;
import org.jparsec.Parser;
import org.springframework.stereotype.Component;

@Component
public class ValueParser {

  private final Parser<Value> booleanParser =
      or(stringCaseInsensitive("FALSE").retn(false), stringCaseInsensitive("TRUE").retn(true))
          .label("boolean")
          .map(Value::new);

  private final Parser<Value> nullParser =
      stringCaseInsensitive("NULL").retn(Value.NULL_VALUE).label("null");

  private final Parser<Value> stringParser =
      or(SINGLE_QUOTE_STRING, DOUBLE_QUOTE_STRING)
          .map(s -> new Value(s.replaceAll("\'", "").replaceAll("\"", "").trim()))
          .label("string");

  private final Parser<Value> numberParser =
      or(
              longer(INTEGER.map(Long::valueOf), DECIMAL.map(Double::valueOf)),
              string("-").next(DECIMAL.map(n -> -Double.valueOf(n))))
          .label("number")
          .map(Value::new);

  private final Parser<Value> singleValueParser =
      between(
          WHITESPACES.skipMany(),
          or(booleanParser, nullParser, numberParser, stringParser),
          WHITESPACES.skipMany());

  private final Parser<Value> inValueParser =
      between(isChar('['), singleValueParser.sepBy(isChar(',')), isChar(']'))
          .label("[]")
          .map(Value::new);

  private final Parser<Value> valueParser = or(inValueParser, singleValueParser);
  private final Parser<Value> likeValueParser = stringParser.label("like").map(LikeValue::new);
  private final Parser<Value> rangeValueParser = inValueParser.label("range").map(RangeValue::new);

  Parser<Value> get() {
    return valueParser;
  }

  Parser<Value> getLikeValue() {
    return likeValueParser;
  }

  Parser<Value> getRangeValue() {
    return rangeValueParser;
  }

  Parser<GeoPointValue> getGeoPointValue(GeoPoint.Type type) {
    switch (type) {
      case VIEWPORT:
      case POLYGON:
        return between(
                isChar('['),
                get().sepBy1(between(WHITESPACES.skipMany(), isChar(','), WHITESPACES.skipMany())),
                isChar(']'))
            .label(type.name())
            .map(values -> new GeoPointValue(values, type));
      case SINGLE:
        return get().label(type.name()).map(value -> new GeoPointValue(value, type));
      default:
        throw new IllegalArgumentException("Invalid GeoPoint type");
    }
  }

  Parser<GeoPointRadiusValue> getGeoPointRadiusValue() {
    return sequence(
        get().label(RADIUS.name()).map(value -> new GeoPointValue(value, SINGLE)),
        createDistanceParser(),
        GeoPointRadiusValue::new);
  }

  private Parser<Optional<Value>> createDistanceParser() {
    return sequence(
            between(
                WHITESPACES.skipMany(), stringCaseInsensitive("DISTANCE"), WHITESPACES.skipMany()),
            between(WHITESPACES.skipMany(), string(":"), WHITESPACES.skipMany()),
            stringParser.label("distance"))
        .asOptional();
  }

  public static class GeoPoint {
    public enum Type {
      VIEWPORT(2, 2),
      POLYGON(3, 1000),
      SINGLE(1, 1);

      private final int minSize;
      private final int maxSize;

      Type(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
      }

      public int getMinSize() {
        return minSize;
      }

      public int getMaxSize() {
        return maxSize;
      }
    }
  }
}
