package com.grupozap.search.api.model.parser;

import static org.jparsec.Parser.newReference;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;
import static org.jparsec.Scanners.isChar;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.*;
import org.jparsec.Parser;

public class QueryParser {

  private final Parser<QueryFragment> queryParser;
  private final Parser<QueryFragment> recursiveQueryParser;

  public QueryParser(
      OperatorParser operatorParser, FilterParser filterParser, NotParser notParser) {
    queryParser =
        sequence(
            operatorParser.getLogicalOperatorParser().asOptional(),
            filterParser.get(),
            QueryFragmentItem::new);

    Parser.Reference<QueryFragment> ref = newReference();
    Parser<QueryFragment> lazy = ref.lazy();
    recursiveQueryParser =
        lazy.between(isChar('('), isChar(')'))
            .or(
                or(
                    queryParser,
                    operatorParser.getLogicalOperatorParser().map(QueryFragmentOperator::new),
                    notParser.get().many().map(QueryFragmentNot::new)))
            .many()
            .label("query")
            .map(QueryFragmentList::new);
    ref.set(recursiveQueryParser);
  }

  Parser<QueryFragment> getRecursiveQueryParser() {
    return recursiveQueryParser;
  }

  @Trace
  public QueryFragment parse(String string) {
    return recursiveQueryParser.parse(string);
  }
}
