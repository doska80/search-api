package com.vivareal.search.api.model.parser;

import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.Field;
import java.util.List;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacetParser {

  private final Parser<List<Field>> facetParser;

  @Autowired
  public FacetParser(FieldParser fieldParser) {
    facetParser =
        fieldParser
            .getWithoutNot()
            .sepBy1(isChar(',').next(WHITESPACES.skipMany()))
            .label("multiple fields");
  }

  @Trace
  public List<Field> parse(String string) {
    return facetParser.parse(string);
  }
}
