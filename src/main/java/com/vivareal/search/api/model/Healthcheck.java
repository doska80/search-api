package com.vivareal.search.api.model;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;

public class Healthcheck {
    private TransportClient client;

    public Healthcheck(final TransportClient client) {
        this.client = client;
    }

    public boolean isStatus() throws InterruptedException, ExecutionException, TimeoutException {
        ActionFuture<ClusterHealthResponse> healthFuture = client.admin().cluster().health(Requests.clusterHealthRequest());
        ClusterHealthResponse healthResponse = healthFuture.get(5, TimeUnit.SECONDS); // TODO timeout parametrizavel

        // TODO imprimir na resposta o healthResponse.getStatus().toString()
        if(healthResponse.getStatus().equals(ClusterHealthStatus.RED))
            throw new ExecutionException(new Exception(healthResponse.getStatus().toString()));

        return true;
    }
}
