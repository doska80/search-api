package com.vivareal.search.api.model.parser;

import static org.jparsec.Parsers.between;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.string;

import org.jparsec.Parser;
import org.springframework.stereotype.Component;

@Component
public class NotParser {

  private final Parser<Boolean> notParser =
      between(
          WHITESPACES.skipMany(), string("NOT").succeeds().label("not"), WHITESPACES.skipMany());

  Parser<Boolean> get() {
    return notParser;
  }
}
