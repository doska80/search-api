package com.vivareal.search.api.model.parser;

import static java.lang.String.valueOf;
import static org.jparsec.Parsers.*;
import static org.jparsec.Scanners.*;

import com.vivareal.search.api.model.query.GeoPointValue;
import com.vivareal.search.api.model.query.LikeValue;
import com.vivareal.search.api.model.query.RangeValue;
import com.vivareal.search.api.model.query.Value;
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

  Parser<Value> get() {
    return valueParser;
  }

  private final Parser<Value> likeValueParser = stringParser.label("like").map(LikeValue::new);

  Parser<Value> getLikeValue() {
    return likeValueParser;
  }

  private final Parser<Value> rangeValueParser = inValueParser.label("range").map(RangeValue::new);

  Parser<Value> getRangeValue() {
    return rangeValueParser;
  }

  Parser<Value> getGeoPointValue(GeoPoint.Type type) {
    return between(
            isChar('['),
            get().sepBy1(between(WHITESPACES.skipMany(), isChar(','), WHITESPACES.skipMany())),
            isChar(']'))
        .label(type.name())
        .map(values -> new GeoPointValue(values, type));
  }

  public static class GeoPoint {
    public enum Type {
      VIEWPORT(2, 2),
      POLYGON(3, 1000);

      Type(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
      }

      private final int minSize;
      private final int maxSize;

      public int getMinSize() {
        return minSize;
      }

      public int getMaxSize() {
        return maxSize;
      }
    }
  }
}
