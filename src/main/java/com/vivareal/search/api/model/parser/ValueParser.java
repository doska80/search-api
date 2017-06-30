package com.vivareal.search.api.model.parser;


import com.vivareal.search.api.model.query.Value;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

import static org.jparsec.Scanners.WHITESPACES;

public class ValueParser {
    private static final Parser<Value> BOOLEAN = Parsers.or(Scanners.stringCaseInsensitive("FALSE").retn(false), Scanners.stringCaseInsensitive("TRUE").retn(true)).label("boolean").map(Value::new);

    private static final Parser<Value> NULL = Scanners.stringCaseInsensitive("NULL").retn(Value.NULL_VALUE).label("null");

    private static final Parser<Value> STRING = Parsers.or(Scanners.SINGLE_QUOTE_STRING, Scanners.DOUBLE_QUOTE_STRING).map(s -> new Value(String.valueOf(s.replaceAll("\'", "").replaceAll("\"", "").trim()))).label("string");

    private static final Parser<Value> NUMBER = Parsers.or(Parsers.longer(Scanners.INTEGER.map(Integer::valueOf), Scanners.DECIMAL.map(Double::valueOf)), Scanners.string("-").next(Scanners.DECIMAL.map(n -> -Double.valueOf(n)))).label("number").map(Value::new);

    private static final Parser<Value> VALUE = Parsers.between(WHITESPACES.skipMany(), Parsers.or(BOOLEAN, NULL, NUMBER, STRING), WHITESPACES.skipMany());

    private static final Parser<Value> VALUE_IN = Parsers
        .between(Scanners.isChar('['), VALUE.sepBy(Scanners.isChar(',')).sepBy(Scanners.isChar(';')), Scanners.isChar(']'))
        .label("[]")
        .map(Value::new);

    private static final Parser<Value> VALUE_PARSER = Parsers.or(VALUE_IN, VALUE);

    static Parser<Value> get() {
        return VALUE_PARSER;
    }
}
