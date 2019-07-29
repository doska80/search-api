package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX_ALIAS;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class IndexAliasesIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void validateIfAliasIsWorkingForGetById() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX_ALIAS + "/1")
        .then()
        .body("id", is("1"));
  }

  @Test
  public void validateIfAliasIsWorkingWithDifferentBehaviorBecauseTheFilterOnProperties() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX_ALIAS)
        .then()
        .body("totalCount", equalTo(10));
  }

  @Test
  public void validateIfAliasIsWorkingBySearchWithFilters() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX_ALIAS + "?filter=id:1")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata", hasSize(1))
        .body("result.testdata[0].id", equalTo(String.valueOf(1)));
  }
}
