package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

public class FieldParser {

    private static final Parser<Field> SIMPLE_KEYWORD_PARSER = Scanners.IDENTIFIER.sepBy1(Scanners.isChar('.')).label("field").map(Field::new);

    private static final Parser<Field> SIMPLE_KEYWORD_PARSER_WITH_NOT = Parsers.sequence(NotParser.get(), SIMPLE_KEYWORD_PARSER, Field::new);

    static Parser<Field> get() {
        return SIMPLE_KEYWORD_PARSER;
    }

    static Parser<Field> getWithoutNot() {
        return SIMPLE_KEYWORD_PARSER_WITH_NOT;
    }
}
