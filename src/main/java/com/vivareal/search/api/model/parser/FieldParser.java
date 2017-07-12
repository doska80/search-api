package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;

import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.IDENTIFIER;
import static org.jparsec.Scanners.isChar;

public class FieldParser {

    private static final Parser<Field> SIMPLE_KEYWORD_PARSER = IDENTIFIER.sepBy1(isChar('.')).label("field").map(Field::new);

    private static final Parser<Field> SIMPLE_KEYWORD_PARSER_WITH_NOT = sequence(NotParser.get(), SIMPLE_KEYWORD_PARSER, Field::new);

    static Parser<Field> get() {
        return SIMPLE_KEYWORD_PARSER;
    }

    static Parser<Field> getWithoutNot() {
        return SIMPLE_KEYWORD_PARSER_WITH_NOT;
    }
}
