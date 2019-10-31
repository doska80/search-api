package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RadiusDistanceFilterTest extends SearchApiIntegrationTest {

  @Test
  public void validateFilterByUnitDistanceUsingTheDefaultDistance() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?filter=geo RADIUS [1.0,-1.0]")
        .then()
        .body("totalCount", equalTo(1));
  }

  @Test
  public void validateFilterByUnitDistanceInformingTheUnitDistance() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?filter=geo RADIUS [1.0,-1.0] DISTANCE:'1000km'")
        .then()
        .body("totalCount", equalTo(7));
  }

  @Test
  public void shouldReturnBadRequestWhenTheClientInformAnInvalidUnitDistance() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(TEST_DATA_INDEX + "?filter=geo RADIUS [1.0,-1.0] DISTANCE:'1000KM'");
  }

  @Test
  public void shouldReturnBadRequestWhenTheClientInformAnInvalidCoordinates() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(TEST_DATA_INDEX + "?filter=geo RADIUS [1,-1] DISTANCE:'10km'");
  }
}
