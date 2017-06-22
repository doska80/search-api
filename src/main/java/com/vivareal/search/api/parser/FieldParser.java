package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

public class FieldParser {
    private static final Parser<Field> SIMPLE_KEYWORD_PARSER = Parsers.sequence(NotParser.get(), Scanners.IDENTIFIER.sepBy(Scanners.isChar('.')), Field::new);

    static Parser<Field> get() {
        return SIMPLE_KEYWORD_PARSER;
    }
}
