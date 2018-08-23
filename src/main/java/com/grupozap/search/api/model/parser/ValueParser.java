package com.grupozap.search.api.model.parser;

import static java.lang.String.valueOf;
import static org.jparsec.Parsers.*;
import static org.jparsec.Scanners.*;

import com.grupozap.search.api.model.query.GeoPointValue;
import com.grupozap.search.api.model.query.LikeValue;
import com.grupozap.search.api.model.query.RangeValue;
import com.grupozap.search.api.model.query.Value;
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
          .map(s -> new Value(valueOf(s.replaceAll("\'", "").replaceAll("\"", "").trim())))
          .label("string");

  private final Parser<Value> numberParser =
      or(
              longer(INTEGER.map(Integer::valueOf), DECIMAL.map(Double::valueOf)),
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
