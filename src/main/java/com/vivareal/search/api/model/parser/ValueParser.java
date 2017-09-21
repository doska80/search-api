package com.vivareal.search.api.model.parser;


import com.vivareal.search.api.model.query.LikeValue;
import com.vivareal.search.api.model.query.RangeValue;
import com.vivareal.search.api.model.query.Value;
import com.vivareal.search.api.model.query.ViewportValue;
import org.jparsec.Parser;

import static java.lang.String.valueOf;
import static org.jparsec.Parsers.*;
import static org.jparsec.Scanners.*;

public class ValueParser {
    private static final Parser<Value> BOOLEAN = or(stringCaseInsensitive("FALSE").retn(false), stringCaseInsensitive("TRUE").retn(true)).label("boolean").map(Value::new);

    private static final Parser<Value> NULL = stringCaseInsensitive("NULL").retn(Value.NULL_VALUE).label("null");

    private static final Parser<Value> STRING = or(SINGLE_QUOTE_STRING, DOUBLE_QUOTE_STRING).map(s -> new Value(valueOf(s.replaceAll("\'", "").replaceAll("\"", "").trim()))).label("string");

    private static final Parser<Value> NUMBER = or(longer(INTEGER.map(Integer::valueOf), DECIMAL.map(Double::valueOf)), string("-").next(DECIMAL.map(n -> -Double.valueOf(n)))).label("number").map(Value::new);

    private static final Parser<Value> VALUE = between(WHITESPACES.skipMany(), or(BOOLEAN, NULL, NUMBER, STRING), WHITESPACES.skipMany());

    private static final Parser<Value> VALUE_IN =
        between(isChar('['), VALUE.sepBy(isChar(',')), isChar(']'))
            .label("[]")
            .map(Value::new);

    private static final Parser<Value> VALUE_PARSER = or(VALUE_IN, VALUE);

    static Parser<Value> get() {
        return VALUE_PARSER;
    }

    public static class Viewport {
        private static final Parser<Value> VALUE_VIEWPORT =
            between(isChar('['), ValueParser.get().sepBy1(isChar(',')).sepBy1(isChar(';')), isChar(']'))
            .label("viewport")
            .map(ViewportValue::new);

        static Parser<Value> get() {
            return VALUE_VIEWPORT;
        }
    }

    public static class Like {
        private static final Parser<Value> VALUE_LIKE = STRING.label("like").map(LikeValue::new);

        static Parser<Value> get() {
            return VALUE_LIKE;
        }
    }

    public static class Range {
        private static final Parser<Value> VALUE_RANGE = VALUE_IN.label("range").map(RangeValue::new);

        static Parser<Value> get() {
            return VALUE_RANGE;
        }
    }
}
