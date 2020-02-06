package com.grupozap.search.api.itest.configuration.es;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class ESIntegrationTestSetup {

  private static final Logger LOG = LoggerFactory.getLogger(ESIntegrationTestSetup.class);

  private static final String INDEXES_FILE = "/es/bootstrap.json";

  private final StrSubstitutor boostrapVariables;
  private final ESIndexHandler esIndexHandler;
  private Map<String, Object> boostrapConfiguration;

  @Autowired
  public ESIntegrationTestSetup(
      @Value("${es.hostname}") String elasticSearchHost,
      @Value("${es.rest.port}") String elasticSearchRestPort,
      ESIndexHandler esIndexHandler) {
    Map<String, String> bootVariables = new HashMap<>();
    bootVariables.put("es_host", elasticSearchHost);
    bootVariables.put("es_port", elasticSearchRestPort);

    this.boostrapVariables = new StrSubstitutor(bootVariables);
    this.boostrapConfiguration = new LinkedHashMap<>();
    this.esIndexHandler = esIndexHandler;
  }

  @PostConstruct
  public void configESForIntegrationTest() {
    readConfigurationFile();
    executeConfigurationCommands();
    warmUp();
  }

  private void readConfigurationFile() {
    try {
      LOG.info("Parsing configuration file");
      this.boostrapConfiguration =
          new ObjectMapper().readValue(getBoostrapConfig(), new TypeReference<>() {});
    } catch (IOException e) {
      throw new RuntimeException("Cannot read es boostrap file config", e);
    }
  }

  private String getBoostrapConfig() throws IOException {
    LOG.info("Reading configuration file: '" + INDEXES_FILE + "'");
    try (var buffer =
        new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(INDEXES_FILE)))) {
      return buffer.lines().collect(joining("\n"));
    }
  }

  private void executeConfigurationCommands() {
    var items = (List<Object>) firstNonNull(boostrapConfiguration.get("item"), emptyList());
    items.stream()
        .map(item -> (Map<String, Object>) item)
        .forEach(
            item -> {
              LOG.info("Loading command: " + item.get("name"));
              ofNullable(item.get("request"))
                  .map(req -> (Map<String, Object>) req)
                  .ifPresent(this::executeSingleCommand);
            });
  }

  private void executeSingleCommand(Map<String, Object> req) {
    var url = createUrlRawText(req.get("url").toString());
    var method = HttpMethod.valueOf(req.get("method").toString());
    List<Header> headers =
        ofNullable(req.get("header")).map(item -> (List<Map<String, String>>) item)
            .orElse(emptyList()).stream()
            .map(item -> new BasicHeader(item.get("key"), item.get("value")))
            .collect(toList());
    var body =
        ofNullable(req.get("body"))
            .map(item -> (Map<String, String>) item)
            .map(item -> item.get("raw"))
            .orElse("");

    LOG.info(
        format(
            "Executing single command: [%s] [%s] [%s] [%s]",
            url, method, headers.toString(), body));

    try (var restClient =
        RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol())).build()) {

      final var request = new Request(method.name(), url.getPath());
      request.setEntity(new NStringEntity(body, APPLICATION_JSON));

      final var options = RequestOptions.DEFAULT.toBuilder();
      headers.forEach(header -> options.addHeader(header.getName(), header.getValue()));
      request.setOptions(options);

      final var response = restClient.performRequest(request);

      LOG.info("Response: " + response + " -- " + body);
    } catch (IOException e) {
      LOG.error("Error setting configuration ", e);
    }
  }

  private void warmUp() {
    try {
      LOG.debug("Starting to add some documents in order to warm up the test data index");
      esIndexHandler.addStandardTestData();
      esIndexHandler.addStandardProperties();

      esIndexHandler.truncateIndexData(TEST_DATA_INDEX);
      esIndexHandler.truncateIndexData(SEARCH_API_PROPERTIES_INDEX);
    } catch (IOException e) {
      LOG.error("Unable to warm up default index data");
    }
  }

  private URL createUrlRawText(String url) {
    try {
      return new URL(boostrapVariables.replace(url));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
