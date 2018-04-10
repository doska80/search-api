package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static com.vivareal.search.api.model.query.Facet._COUNT;
import static com.vivareal.search.api.model.query.Facet._KEY;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.aggregations.BucketOrder.count;
import static org.elasticsearch.search.aggregations.BucketOrder.key;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.query.Item;
import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.search.Facetable;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacetQueryAdapter {

  private final FacetParser facetParser;
  private IndexSettings indexSettings; // Request scoped

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
            facet -> {
              AggregationBuilder agg =
                  terms(facet.getField().getName())
                      .field(facet.getField().getName())
                      .size(facetSize)
                      .shardSize(indexSettings.getShards())
                      .order(facetOrder(facet.getSort().getFirst()));

              if (FIELD_TYPE_NESTED.typeOf(facet.getField().getTypeFirstName()))
                applyFacetsByNestedFields(searchRequestBuilder, facet.getField().firstName(), agg);
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

  private BucketOrder facetOrder(Item item) {
    boolean isASC = item.getOrderOperator().equals(OrderOperator.ASC);

    switch (item.getField().getName()) {
      case _KEY:
        return key(isASC);
      case _COUNT:
      default:
        return count(isASC);
    }
  }
}
