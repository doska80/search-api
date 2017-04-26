package com.vivareal.search.api.interceptor;

import com.vivareal.search.api.controller.v2.stream.ResponseStream;
import com.vivareal.search.api.model.SearchApiIterator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class StreamingResponseInterceptor implements HandlerInterceptor {

    @Autowired
    private TransportClient client;

    @Override
    public boolean preHandle(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Object handler) throws Exception {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(httpRequest.getParameter("q")); // TODO gets a standard query paramater
        boolQuery.must().add(queryString);

        SearchRequestBuilder core = client.prepareSearch("inmuebles")
                .setSize(100) // TODO we must configure timeouts
                .setScroll(new TimeValue(60000));
        core.setQuery(boolQuery);

        SearchResponse response = core.get();

        httpResponse.setContentType("application/x-ndjson");

        ResponseStream.create(httpResponse.getOutputStream())
                .withIterator(new SearchApiIterator<>(client, response), SearchHit::source);

        return false;
    }
}
