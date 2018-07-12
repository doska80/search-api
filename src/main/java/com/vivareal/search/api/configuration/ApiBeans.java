package com.vivareal.search.api.configuration;

import static java.net.InetAddress.getAllByName;
import static org.elasticsearch.client.RestClient.builder;
import static org.elasticsearch.client.transport.TransportClient.CLIENT_TRANSPORT_SNIFF;
import static org.elasticsearch.cluster.ClusterName.CLUSTER_NAME_SETTING;
import static org.elasticsearch.common.settings.Settings.builder;
import static org.elasticsearch.transport.Transport.TRANSPORT_TCP_COMPRESS;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import com.vivareal.search.api.serializer.ESResponseSerializer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
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

  private RestClient restClient = null;
  private TransportClient esClient = null;
  private RestHighLevelClient restHighLevelClient = null;

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
        builder()
            .put(CLIENT_TRANSPORT_SNIFF.getKey(), true)
            .put(TRANSPORT_TCP_COMPRESS.getKey(), true)
            .put(CLUSTER_NAME_SETTING.getKey(), clusterName)
            .build();

    this.esClient = new PreBuiltTransportClient(settings);

    for (InetAddress address : getAllByName(hostname))
      this.esClient.addTransportAddress(new TransportAddress(address, port));

    return esClient;
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public RestHighLevelClient restHighLevelClient(
      @Value("${es.client.socket.timeout}") int socketTimeout,
      @Value("${es.client.conn.timeout}") int connTimeout,
      @Value("${es.client.conn.request.timeout}") int connRequestTimeout,
      @Value("${es.client.max.retry.timeout}") int maxRetryTimeout,
      @Value("${es.client.http.max.conn.total}") int maxConnTotal,
      @Value("${es.client.http.max.conn.per-route}") int maxConnPerRoute) {
    this.restHighLevelClient =
        new RestHighLevelClient(
            builder(new HttpHost(hostname, restPort, "http"))
                .setHttpClientConfigCallback(
                    http -> http.setMaxConnTotal(maxConnTotal).setMaxConnPerRoute(maxConnPerRoute))
                .setRequestConfigCallback(
                    requestConfigBuilder ->
                        requestConfigBuilder
                            .setConnectionRequestTimeout(connRequestTimeout)
                            .setConnectTimeout(connTimeout)
                            .setSocketTimeout(socketTimeout))
                .setMaxRetryTimeoutMillis(maxRetryTimeout));
    return restHighLevelClient;
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
    if (esClient != null) esClient.close();
    if (restHighLevelClient != null) restHighLevelClient.close();
    if (restClient != null) restClient.close();
  }
}
