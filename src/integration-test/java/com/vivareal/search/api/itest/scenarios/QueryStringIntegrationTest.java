package com.vivareal.search.api.itest.scenarios;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringRunner.class)
public class QueryStringIntegrationTest extends SearchApiIntegrationTest {

    @Test
    public void shouldReturnOneResultWhenUsingQParameter() {
        given()
        .log().all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?q='string with char a'&fields=object.string&mm=100%")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata.id.get(0)", equalTo("1"));
    }

    @Test
    public void shouldReturnOneResultWhenCombineQParameterWithValidFilter() {
        given()
        .log().all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?q='string with char i'&fields=object.string&filter=numeric < 10&mm=100%")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata.id.get(0)", equalTo("9"));
    }

    @Test
    public void shouldReturnZeroResultWhenCombineQParameterWithInvalidFilter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?q='string with char i'&fields=object.string&filter=numeric < 5&mm=100%")
        .then()
            .body("totalCount", equalTo(0));
    }
}
