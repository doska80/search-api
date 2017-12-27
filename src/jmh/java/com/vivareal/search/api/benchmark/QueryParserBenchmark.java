package com.vivareal.search.api.benchmark;

import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.model.query.QueryFragment;
import org.jparsec.Parser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class QueryParserBenchmark {

    @State(Scope.Benchmark)
    public static class QueryState {
        OperatorParser operatorParser = new OperatorParser();
        NotParser notParser = new NotParser();
        FilterParser filterParser = new FilterParser(new FieldParser(notParser), operatorParser, new ValueParser());
        final QueryParser parser = new QueryParser(operatorParser, filterParser, notParser);
    }

    @Benchmark
    public void simpleQuery(QueryState state) {
        state.parser.parse("a = 1");
    }

    @Benchmark
    public void recursiveQuery(QueryState state) {
        state.parser.parse("rooms:3 AND pimba:2 AND(suites=1 OR (parkingLots IN [1,\"abc\"] AND xpto <> 3))");
    }
}
