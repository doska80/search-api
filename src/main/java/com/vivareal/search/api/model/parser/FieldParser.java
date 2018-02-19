package com.vivareal.search.api.model.parser;

import static java.lang.String.join;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.IDENTIFIER;
import static org.jparsec.Scanners.isChar;

import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FieldParser {

  private final Parser<Field> fieldParser;
  private final Parser<Field> fieldParserWithNot;

  @Autowired
  public FieldParser(NotParser notParser, FieldFactory fieldFactory) {
    fieldParser =
        IDENTIFIER
            .sepBy1(isChar('.'))
            .label("field")
            .map(field -> join(".", field))
            .map(fieldFactory::createField);
    fieldParserWithNot = sequence(notParser.get(), fieldParser, fieldFactory::createField);
  }

  Parser<Field> get() {
    return fieldParser;
  }

  Parser<Field> getWithoutNot() {
    return fieldParserWithNot;
  }

  public Field parse(String string) {
    return fieldParser.parse(string);
  }
}
