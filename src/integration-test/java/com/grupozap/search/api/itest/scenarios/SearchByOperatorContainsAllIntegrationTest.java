package com.grupozap.search.api.itest.scenarios;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SearchByOperatorContainsAllIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void shouldExecuteContainsAllFilter() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?filter=array_integer CONTAINS_ALL [18,19,20]&facets=array_integer")
        .then()
        .body("totalCount", equalTo(11))
        .body("result.testdata", hasSize(11))
        .body("result.facets.array_integer.18", equalTo(11))
        .body("result.facets.array_integer.19", equalTo(11))
        .body("result.facets.array_integer.20", equalTo(11));
  }

  @Test
  public void shouldExecuteNotContainsAllFilter() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(
            TEST_DATA_INDEX
                + "?filter=NOT array_integer CONTAINS_ALL [18,19,20]&facets=array_integer")
        .then()
        .body("totalCount", equalTo(17))
        .body("result.testdata", hasSize(17))
        .body("result.facets.array_integer.18", nullValue())
        .body("result.facets.array_integer.19", nullValue())
        .body("result.facets.array_integer.20", nullValue());
  }
}
