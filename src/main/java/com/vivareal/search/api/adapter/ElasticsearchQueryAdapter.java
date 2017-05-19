package com.vivareal.search.api.adapter;


import com.vivareal.search.api.model.SearchApiIndex;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.Filter;
import com.vivareal.search.api.parser.QueryFragment;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchHit, List<QueryFragment>, List<Sort>> {

    public static final String INDEX = "inmuebles";

    private final TransportClient transportClient;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public Object getById(SearchApiRequest request, String id) {
        SearchApiIndex index = SearchApiIndex.of(request);

        // FIXME we need to receive index and type separately somehow
        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(index.getIndex()).setType(index.getIndex()).setId(id);
        try {
            GetResponse response = requestBuilder.execute().get(1, TimeUnit.SECONDS);
            return response.getSource();
        } catch (Exception e) {
            e.printStackTrace(); // FIXME seriously, fix me.
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getQueryMarcao(SearchApiRequest request) {
        List<Map<String, Object>> response = new ArrayList<>();
        SearchRequestBuilder searchBuilder = transportClient.prepareSearch("inmuebles"); // FIXME parameter
        searchBuilder.setPreference("_replica_first"); // <3

        BoolQueryBuilder query = new BoolQueryBuilder();

        if (request.getFilter().isEmpty()) {
            BoolQueryBuilder filterQuery = new BoolQueryBuilder();
            request.getFilter().forEach(filterFragment -> {
                QueryFragment.Type type = filterFragment.getType();
                if (QueryFragment.Type.EXPRESSION_LIST.equals(type)) { // TODO sub query, still not workiung :(
                    throw new UnsupportedOperationException("Subqueries aren't supported yet :("); // TODO TUDO!
                } else if (QueryFragment.Type.FILTER.equals(type)) {
                    Filter filter = filterFragment.get();
                    filterQuery.must().add(QueryBuilders.matchQuery(filter.getField().getName(), filter.getValue()));
                } else {
                    System.out.println(filterFragment);
                }
            });
            query.filter(filterQuery);
        }
        searchBuilder.setQuery(query);
        System.out.println(searchBuilder);
        searchBuilder.execute().actionGet().getHits().forEach(hit -> {  // FIXME should be async if possible
            response.add(hit.getSource()); // FIXME avoid iterating twice!
        });
        return response;
    }

    @Override
    protected List<QueryFragment> getFilter(List<String> filter) {
        return null;
    }

    @Override
    protected List<Sort> getSort(List<String> sort) {
        return null;
    }

}
