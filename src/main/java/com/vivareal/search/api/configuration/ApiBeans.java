package com.vivareal.search.api.configuration;

import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import com.vivareal.search.api.serializer.ESResponseSerializer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.vivareal.search.api.configuration.ThreadPoolConfig.MIN_SIZE;
import static com.vivareal.search.api.configuration.ThreadPoolConfig.QUEUE_SIZE;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration
@EnableScheduling
public class ApiBeans implements DisposableBean {

    private TransportClient esClient = null;
    private RestClient restClient = null;

    @Value("${es.hostname}")
    private String hostname;

    @Value("${es.port}")
    private Integer port;

    @Value("${es.rest.port}")
    private Integer restPort;

    @Value("${es.cluster.name}")
    private String clusterName;

    @Bean
    @Scope(SCOPE_SINGLETON)
    public TransportClient transportClient() throws UnknownHostException {
        Settings settings = Settings.builder()
                .put("client.transport.nodes_sampler_interval", "5s")
                .put("client.transport.sniff", true)
                .put("transport.tcp.compress", true)
                .put("cluster.name", clusterName)
                .put("request.headers.X-Found-Cluster", "${cluster.name}")
                .put("thread_pool.search.size", MIN_SIZE)
                .put("thread_pool.search.queue_size", QUEUE_SIZE)
                .put("thread_pool.get.size", MIN_SIZE)
                .put("thread_pool.get.queue_size", QUEUE_SIZE)
                .build();
        this.esClient = new PreBuiltTransportClient(settings);

        for (InetAddress address : InetAddress.getAllByName(hostname))
            this.esClient.addTransportAddress(new InetSocketTransportAddress(address, port));
        return esClient;
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    public RestClient restClient() {
        restClient = RestClient.builder(new HttpHost(hostname, restPort, "http")).build();
        return restClient;
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    public Jackson2ObjectMapperBuilderCustomizer addCustomSearchResponseDeserialization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.serializerByType(SearchResponseEnvelope.class, new ESResponseSerializer());
    }

    @Override
    public void destroy() throws Exception {
        if (this.esClient != null)
            this.esClient.close();

        if (restClient != null)
            restClient.close();
    }
}
