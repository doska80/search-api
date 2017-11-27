package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.IDENTIFIER;
import static org.jparsec.Scanners.isChar;

@Component
public class FieldParser {

    private final Parser<Field> simpleKeywordParser;
    private final Parser<Field> simpleKeywordParserWithNot;

    @Autowired
    public FieldParser(NotParser notParser) {
        simpleKeywordParser = IDENTIFIER.sepBy1(isChar('.')).label("field").map(Field::new);
        simpleKeywordParserWithNot = sequence(notParser.get(), simpleKeywordParser, Field::new);
    }

    Parser<Field> get() {
        return simpleKeywordParser;
    }

    Parser<Field> getWithoutNot() {
        return simpleKeywordParserWithNot;
    }
}
