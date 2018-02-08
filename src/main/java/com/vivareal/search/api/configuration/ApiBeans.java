package com.vivareal.search.api.configuration;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import com.vivareal.search.api.serializer.ESResponseSerializer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

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
    Settings settings =
        Settings.builder()
            .put(TransportClient.CLIENT_TRANSPORT_SNIFF.getKey(), true)
            .put(Transport.TRANSPORT_TCP_COMPRESS.getKey(), true)
            .put(ClusterName.CLUSTER_NAME_SETTING.getKey(), clusterName)
            .build();
    this.esClient = new PreBuiltTransportClient(settings);

    for (InetAddress address : InetAddress.getAllByName(hostname))
      this.esClient.addTransportAddress(new TransportAddress(address, port));
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
    return jacksonObjectMapperBuilder ->
        jacksonObjectMapperBuilder.serializerByType(
            SearchResponseEnvelope.class, new ESResponseSerializer());
  }

  @Override
  public void destroy() throws Exception {
    if (this.esClient != null) this.esClient.close();

    if (restClient != null) restClient.close();
  }
}
