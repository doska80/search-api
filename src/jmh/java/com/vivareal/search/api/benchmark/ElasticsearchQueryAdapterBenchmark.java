package com.vivareal.search.api.benchmark;

import com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter;
import com.vivareal.search.api.adapter.FilterQueryAdapter;
import com.vivareal.search.api.model.http.SearchApiRequestBuilder;
import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.search.Filterable;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

public class ElasticsearchQueryAdapterBenchmark {

    @State(Scope.Benchmark)
    public static class ElasticsearchQueryAdapterState {
        final FilterQueryAdapter adapter = new FilterQueryAdapter(new ElasticsearchSettingsAdapter(null) {
            @Override
            public boolean checkFieldName(String index, String fieldName, boolean acceptAsterisk) {
                return true;
            }

            @Override
            public boolean isTypeOf(String index, String fieldName, MappingType type) {
                return type != FIELD_TYPE_NESTED;
            }
        }, null);
        final BoolQueryBuilder bqb = boolQuery();
        final Filterable filterable = SearchApiRequestBuilder.create().index("tincas").filter("a = 1").build();
    }

    @Benchmark
    public void applyFilterQuery(ElasticsearchQueryAdapterState state) {
        state.adapter.apply(state.bqb, state.filterable);
    }
}
