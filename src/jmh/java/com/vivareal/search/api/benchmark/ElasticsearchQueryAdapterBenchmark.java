package com.vivareal.search.api.benchmark;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import com.vivareal.search.api.adapter.FilterQueryAdapter;
import com.vivareal.search.api.model.http.SearchApiRequestBuilder;
import com.vivareal.search.api.model.search.Filterable;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class ElasticsearchQueryAdapterBenchmark {

  @Benchmark
  public void applyFilterQuery(ElasticsearchQueryAdapterState state) {
    state.adapter.apply(state.bqb, state.filterable);
  }

  @State(Scope.Benchmark)
  public static class ElasticsearchQueryAdapterState {
    final FilterQueryAdapter adapter = new FilterQueryAdapter(queryParserFixture());
    final BoolQueryBuilder bqb = boolQuery();
    final Filterable filterable =
        SearchApiRequestBuilder.create().index("tincas").filter("a = 1").build();
  }
}
