package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SearchAfterIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void validateSortAscUsingSearchAfterToPagination() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?size=5&includeFields=numeric&sort=numeric ASC")
        .then()
        .body("result.testdata.numeric", equalTo(rangeClosed(1, 5).boxed().collect(toList())))
        .body("cursorId", equalTo("5_5"));

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?size=5&includeFields=numeric&sort=numeric ASC&cursorId=5_5")
        .then()
        .body("result.testdata.numeric", equalTo(rangeClosed(6, 10).boxed().collect(toList())))
        .body("cursorId", equalTo("10_10"));
  }

  @Test
  public void shouldReturnErrorOnSearchAfterWhenParametersIsInvalid() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(TEST_DATA_INDEX + "?size=5&includeFields=numeric&sort=numeric ASC,id DESC&cursorId=5");
  }
}
