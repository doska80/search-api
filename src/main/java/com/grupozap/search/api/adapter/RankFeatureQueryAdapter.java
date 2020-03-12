package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_RFQ;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.grupozap.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder.ScoreFunction.Log;
import org.springframework.stereotype.Component;

@Component
public class RankFeatureQueryAdapter {

  public static final String RANK_FEATURE_QUERY_PREFIX = "priority";

  public void apply(BoolQueryBuilder queryBuilder, FilterableApiRequest request) {
    final String fieldFactor = ES_RFQ.getValue(request.getIndex());

    if (!isEmpty(fieldFactor)
        && (!request.isDisableRfq()
            || (request.getSort() != null
                && request.getSort().toLowerCase().contains(RANK_FEATURE_QUERY_PREFIX)))) {

      var fieldFactorValue = fieldFactor.split(":");
      queryBuilder
          .should()
          .add(
              new RankFeatureQueryBuilder(
                  fieldFactorValue[0], new Log(parseInt(fieldFactorValue[1]))));

      /* for empty search filters, we need to include the match_all query builder */
      if (isEmpty(queryBuilder.filter())
          && isEmpty(queryBuilder.must())
          && isEmpty(queryBuilder.mustNot())) {
        queryBuilder.filter().add(matchAllQuery());
      }
    }
  }
}
