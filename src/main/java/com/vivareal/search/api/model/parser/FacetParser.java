package com.vivareal.search.api.model.parser;

import static org.jparsec.Parsers.between;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;
import static org.jparsec.Scanners.string;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.*;
import java.util.List;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacetParser {
  static final String SORT_FACET_FIELD = "sortFacet";

  private final Parser<List<Facet>> facetParser;

  @Autowired
  public FacetParser(FieldParser fieldParser, SortParser sortParser) {

    Parser<Sort> sortFacetParser =
        sequence(
            between(WHITESPACES.skipMany(), string(SORT_FACET_FIELD), WHITESPACES.skipMany()),
            between(WHITESPACES.skipMany(), string(":"), WHITESPACES.skipMany()),
            sortParser.getSingleSortParser(),
            (void1, void2, sort) -> sort);

    this.facetParser =
        sequence(fieldParser.getWithoutNot(), sortFacetParser.asOptional(), Facet::new)
            .sepBy(isChar(',').next(WHITESPACES.skipMany()))
            .label("sort");
  }

  @Trace
  public List<Facet> parse(String string) {
    return facetParser.parse(string);
  }
}
