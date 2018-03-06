package com.vivareal.search.api.benchmark;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import com.vivareal.search.api.adapter.FilterQueryAdapter;
import com.vivareal.search.api.model.http.SearchApiRequestBuilder;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.search.Filterable;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class ElasticsearchQueryAdapterBenchmark {

  @Benchmark
  public void applyFilterQuery(ElasticsearchQueryAdapterState state) {
    QueryParser queryParser = queryParserFixture();
    QueryFragment queryFragment = queryParser.parse(state.filterable.getFilter());
    state.adapter.apply(state.bqb, queryFragment, state.filterable.getIndex());
  }

  @State(Scope.Benchmark)
  public static class ElasticsearchQueryAdapterState {
    final FilterQueryAdapter adapter = new FilterQueryAdapter(queryParserFixture());
    final BoolQueryBuilder bqb = boolQuery();
    final Filterable filterable =
        SearchApiRequestBuilder.create().index("tincas").filter("a = 1").build();
  }
}
