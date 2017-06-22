package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Scanners;

public class NotParser {

    public static final Parser<Boolean> NOT_PARSER = Scanners.WHITESPACES.skipMany().next(Scanners.string("NOT").next(Scanners.WHITESPACES.atLeast(1))).succeeds();
    public static final Parser<Boolean> NOT_PARSER2 = Scanners.WHITESPACES.skipMany().next(Scanners.string("NOT2").next(Scanners.WHITESPACES.atLeast(1))).succeeds();

    static Parser<Boolean> get() {
        return NOT_PARSER;
    }

    static Parser<Boolean> get2() {
        return NOT_PARSER2;
    }
}
