package com.grupozap.search.api.model.parser;

import static java.lang.String.join;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.IDENTIFIER;
import static org.jparsec.Scanners.isChar;

import com.grupozap.search.api.service.parser.factory.FieldCache;
import com.grupozap.search.api.service.parser.factory.FieldFactory;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.factory.FieldCache;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jparsec.Parser;

public class FieldParser {

  private final Parser<Field> fieldParser;
  private final Parser<Field> fieldParserWithNot;

  public FieldParser(NotParser notParser) {
    this(notParser, FieldFactory::createField, FieldFactory::createField);
  }

  public FieldParser(NotParser notParser, FieldCache fieldCache) {
    this(notParser, fieldCache::getField, FieldFactory::createField);
  }

  private FieldParser(
      NotParser notParser,
      Function<String, Field> createField,
      BiFunction<Boolean, Field, Field> createFieldWithNot) {
    fieldParser =
        IDENTIFIER
            .sepBy1(isChar('.'))
            .label("field")
            .map(field -> join(".", field))
            .map(createField);
    fieldParserWithNot = sequence(notParser.get(), fieldParser, createFieldWithNot);
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
