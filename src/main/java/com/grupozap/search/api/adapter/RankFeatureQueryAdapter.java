package com.grupozap.search.api.adapter;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.grupozap.search.api.listener.ESSortListener;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder.ScoreFunction.Log;
import org.springframework.stereotype.Component;

@Component
public class RankFeatureQueryAdapter {

  private final ESSortListener esSortListener;

  public RankFeatureQueryAdapter(ESSortListener esSortListener) {
    this.esSortListener = esSortListener;
  }

  public void apply(BoolQueryBuilder queryBuilder, FilterableApiRequest request) {
    this.esSortListener
        .getRfq(request)
        .ifPresent(
            rfq -> {
              queryBuilder
                  .should()
                  .add(
                      new RankFeatureQueryBuilder(rfq.getField(), new Log(rfq.getScalingFactor())));

              /* for empty search filters, we need to include the match_all query builder */
              if (isEmpty(queryBuilder.filter())
                  && isEmpty(queryBuilder.must())
                  && isEmpty(queryBuilder.mustNot())) {
                queryBuilder.filter().add(matchAllQuery());
              }
            });
  }
}
