package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.aggregations.bucket.terms.Terms.Order.count;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.search.Facetable;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacetQueryAdapter {

  private IndexSettings indexSettings; // Request scoped

  private final FacetParser facetParser;

  public FacetQueryAdapter(FacetParser facetParser) {
    this.facetParser = facetParser;
  }

  @Autowired
  public void setIndexSettings(IndexSettings indexSettings) {
    this.indexSettings = indexSettings;
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Facetable request) {
    Set<String> value = request.getFacets();
    if (isEmpty(value)) return;

    final String indexName = request.getIndex();
    request.setFacetingValues(ES_FACET_SIZE.getValue(indexName));

    final int facetSize = request.getFacetSize();
    facetParser
        .parse(value.stream().collect(joining(",")))
        .forEach(
            field -> {
              AggregationBuilder agg =
                  terms(field.getName())
                      .field(field.getName())
                      .size(facetSize)
                      .shardSize(indexSettings.getShards())
                      .order(count(false));

              if (FIELD_TYPE_NESTED.typeOf(field.getTypeFirstName()))
                applyFacetsByNestedFields(searchRequestBuilder, field.firstName(), agg);
              else searchRequestBuilder.addAggregation(agg);
            });
  }

  private void applyFacetsByNestedFields(
      SearchRequestBuilder searchRequestBuilder, final String name, final AggregationBuilder agg) {
    Optional<AggregationBuilder> nestedAgg =
        ofNullable(searchRequestBuilder.request().source())
            .map(SearchSourceBuilder::aggregations)
            .map(Builder::getAggregatorFactories)
            .flatMap(
                aggregations ->
                    aggregations
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
