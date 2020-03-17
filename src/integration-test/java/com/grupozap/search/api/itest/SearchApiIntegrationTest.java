package com.grupozap.search.api.itest;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupozap.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.grupozap.search.api.itest.configuration.data.StandardDatasetAsserts;
import com.grupozap.search.api.itest.configuration.es.ESIndexHandler;
import com.grupozap.search.api.itest.utils.JsonFileUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@RunWith(Enclosed.class)
@TestPropertySource({
  "classpath:application.properties",
  "classpath:application-test.properties",
  "classpath:configuration/application-itest.properties"
})
@ContextConfiguration(classes = SearchApiIntegrationTestContext.class)
public class SearchApiIntegrationTest {

  protected static final ObjectMapper mapper = new ObjectMapper();
  protected JsonFileUtils jsonFileUtils = new JsonFileUtils();

  @Value("${itest.standard.dataset.size}")
  protected Integer standardDatasetSize;

  @Value("${es.facet.size}")
  protected Integer facetSize;

  @Value("${search.api.base.url}")
  protected String baseUrl;

  @Value("${es.default.size}")
  protected Integer defaultPageSize;

  @Autowired protected ESIndexHandler esIndexHandler;
  @Autowired protected StandardDatasetAsserts asserts;

  @BeforeClass
  public static void enableLogging() {
    enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Before
  public void disableAllCircuits() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get("/circuits/actions/state/" + State.DISABLED);
  }

  @After
  public void setupAfter() {
    esIndexHandler.setDefaultProperties();
    esIndexHandler.addStandardProperties();
  }

  @PostConstruct
  public void setupIndexHandler() throws IOException {
    esIndexHandler.truncateIndexData(TEST_DATA_INDEX);
    esIndexHandler.addStandardTestData();

    esIndexHandler.truncateIndexData(SEARCH_API_PROPERTIES_INDEX);
    esIndexHandler.setDefaultProperties();
    esIndexHandler.addStandardProperties();
  }
}
