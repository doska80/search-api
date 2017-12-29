package com.vivareal.search.api.itest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivareal.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.vivareal.search.api.itest.configuration.data.StandardDatasetAsserts;
import com.vivareal.search.api.itest.configuration.es.ESIndexHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static org.apache.http.HttpStatus.SC_OK;

@RunWith(Enclosed.class)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties", "classpath:configuration/application-itest.properties"})
@ContextConfiguration(classes = SearchApiIntegrationTestContext.class)
public class SearchApiIntegrationTest {

    @Value("${itest.standard.dataset.size}")
    protected Integer standardDatasetSize;

    @Value("${es.facet.size}")
    protected Integer facetSize;

    @Value("${search.api.base.url}")
    protected String baseUrl;

    @Value("${es.default.size}")
    protected Integer defaultPageSize;

    @Autowired
    protected ESIndexHandler esIndexHandler;

    @Autowired
    protected StandardDatasetAsserts asserts;

    protected static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setup() {
        loadAll();
    }

    @Before
    public void forceCircuitClosed() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get("/forceClosed/true")
        ;
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
