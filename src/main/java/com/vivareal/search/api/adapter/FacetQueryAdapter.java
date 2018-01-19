package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.search.Facetable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FacetQueryAdapter {

  private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
  private final FacetParser facetParser;

  public FacetQueryAdapter(
      @Qualifier("elasticsearchSettings")
          SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter,
      FacetParser facetParser) {
    this.settingsAdapter = settingsAdapter;
    this.facetParser = facetParser;
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Facetable request) {
    Set<String> value = request.getFacets();
    if (isEmpty(value)) return;

    final String indexName = request.getIndex();
    request.setFacetingValues(ES_FACET_SIZE.getValue(indexName));

    final int facetSize = request.getFacetSize();
    final int shardSize =
        parseInt(String.valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS)));

    facetParser
        .parse(value.stream().collect(joining(",")))
        .forEach(
            facet -> {
              final String fieldName = facet.getName();
              final String firstName = facet.firstName();
              settingsAdapter.checkFieldName(indexName, fieldName, false);

              AggregationBuilder agg =
                  terms(fieldName)
                      .field(fieldName)
                      .size(facetSize)
                      .shardSize(shardSize)
                      .order(Terms.Order.count(false));

              if (settingsAdapter.isTypeOf(indexName, firstName, FIELD_TYPE_NESTED))
                applyFacetsByNestedFields(searchRequestBuilder, firstName, agg);
              else searchRequestBuilder.addAggregation(agg);
            });
  }

  private void applyFacetsByNestedFields(
      SearchRequestBuilder searchRequestBuilder, final String name, final AggregationBuilder agg) {
    Optional<AggregationBuilder> nestedAgg =
        ofNullable(searchRequestBuilder.request().source().aggregations())
            .flatMap(
                builder ->
                    builder
                        .getAggregatorFactories()
                        .stream()
                        .filter(
                            aggregationBuilder ->
                                aggregationBuilder instanceof NestedAggregationBuilder
                                    && name.equals(aggregationBuilder.getName()))
                        .findFirst());

    if (nestedAgg.isPresent()) nestedAgg.get().subAggregation(agg);
    else searchRequestBuilder.addAggregation(nested(name, name).subAggregation(agg));
  }
}
