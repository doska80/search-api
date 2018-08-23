package com.grupozap.search.api.benchmark;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;

import com.grupozap.search.api.model.parser.FilterParser;
import com.grupozap.search.api.model.parser.NotParser;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.parser.ValueParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class QueryParserBenchmark {

  @Benchmark
  public void simpleQuery(QueryState state) {
    state.parser.parse("a = 1");
  }

  @Benchmark
  public void recursiveQuery(QueryState state) {
    state.parser.parse(
        "rooms:3 AND pimba:2 AND(suites=1 OR (parkingLots IN [1,\"abc\"] AND xpto <> 3))");
  }

  @State(Scope.Benchmark)
  public static class QueryState {
    final OperatorParser operatorParser = new OperatorParser();
    final NotParser notParser = new NotParser();
    final FilterParser filterParser =
        new FilterParser(fieldParserFixture(), operatorParser, new ValueParser());
    final QueryParser parser = new QueryParser(operatorParser, filterParser, notParser);
  }
}
