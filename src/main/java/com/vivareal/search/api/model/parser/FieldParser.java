package com.vivareal.search.api.model.parser;

import static java.lang.String.join;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.IDENTIFIER;
import static org.jparsec.Scanners.isChar;

import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.factory.FieldCache;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jparsec.Parser;

public class FieldParser {

  private Parser<Field> fieldParser;
  private Parser<Field> fieldParserWithNot;

  public FieldParser(NotParser notParser, FieldFactory fieldFactory) {
    this(notParser, fieldFactory::createField, fieldFactory::createField);
  }

  public FieldParser(NotParser notParser, FieldFactory fieldFactory, FieldCache fieldCache) {
    this(notParser, fieldCache::getField, fieldFactory::createField);
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
