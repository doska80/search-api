package com.vivareal.search.api.configuration;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration
@EnableScheduling
public class ApiBeans implements DisposableBean {

    private TransportClient esClient = null;

    @Bean
    @Scope(SCOPE_SINGLETON)
    public TransportClient transportClient(
            @Value("${es.hostname}") final String hostname,
            @Value("${es.port}") final Integer port,
            @Value("${es.cluster.name}") final String clusterName) throws UnknownHostException {
        Settings settings = Settings.builder()
                .put("client.transport.nodes_sampler_interval", "5s")
                .put("client.transport.sniff", true)
                .put("transport.tcp.compress", true)
                .put("cluster.name", clusterName)
                .put("request.headers.X-Found-Cluster", "${cluster.name}")
                .build();
        this.esClient = new PreBuiltTransportClient(settings);

        for (InetAddress address : InetAddress.getAllByName(hostname))
            this.esClient.addTransportAddress(new InetSocketTransportAddress(address, port));
        return esClient;
    }

    @Override
    public void destroy() throws Exception {
        if (this.esClient != null)
            this.esClient.close();
    }
}
