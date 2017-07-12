package com.vivareal.search.api.model.parser;

import org.jparsec.Parser;

import static org.jparsec.Parsers.between;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.string;

public class NotParser {
    public static final Parser<Boolean> NOT_PARSER = between(WHITESPACES.skipMany(), string("NOT").succeeds().label("not"), WHITESPACES.skipMany());
    static Parser<Boolean> get() {
        return NOT_PARSER;
    }
}
