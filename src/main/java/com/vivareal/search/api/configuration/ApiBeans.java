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

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PreDestroy;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration
public class ApiBeans implements DisposableBean {

    private TransportClient esClient = null;

    @Bean
    @Scope(SCOPE_SINGLETON)
    public TransportClient elasticSearchClientBean(
            @Value("${es.hostname}") final String hostname,
            @Value("${es.port}") final Integer port,
            @Value("${es.cluster.name}") final String clusterName,
            @Value("${es.tcp.compress}") final boolean tcpCompress) throws UnknownHostException {
        Settings settings = Settings.builder()
                .put("client.transport.sniff", false)
                .put("transport.tcp.compress", tcpCompress)
                .put("cluster.name", clusterName)
                .build();
        InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(hostname), port);
        this.esClient = new PreBuiltTransportClient(settings).addTransportAddress(address);
        return esClient;
    }

    @Override
    public void destroy() throws Exception {
        if (this.esClient != null)
            this.esClient.close();
    }
}
