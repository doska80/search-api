package com.vivareal.search.api.itest.scenarios;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class FacetIntegrationTest extends SearchApiIntegrationTest {
  @Test
  public void validateFacetFields() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?facets=id,array_integer,isEven")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata", everyItem(notNullValue()))
        .body("result.facets", notNullValue())
        .body("result.facets.id.findAll { it.value == 1 }.size()", equalTo(facetSize))
        .body("result.facets.isEven.true", equalTo(standardDatasetSize / 2))
        .body("result.facets.isEven.false", equalTo(standardDatasetSize / 2))
        .body(
            "result.facets.array_integer.findAll { it.key.toInteger() + it.value - 1 == "
                + standardDatasetSize
                + " }.size()",
            equalTo(facetSize));
  }

  @Test
  public void validateFacetSize() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?facets=id,array_integer&facetSize=" + standardDatasetSize)
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata", everyItem(notNullValue()))
        .body("result.facets", notNullValue())
        .body("result.facets.id.findAll { it.value == 1 }.size()", equalTo(standardDatasetSize))
        .body(
            "result.facets.array_integer.findAll { it.key.toInteger() + it.value - 1 == "
                + standardDatasetSize
                + " }.size()",
            equalTo(standardDatasetSize));
  }

  @Test
  public void validateSearchWithNonExistingFacet() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(format("%s?facets=non_existing_field", TEST_DATA_INDEX));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void validateFacetSortPosition() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(
                format(
                    "%s?size=0&facets=facetString,facetInteger,facetBoolean,array_integer&facetSize=%s",
                    TEST_DATA_INDEX, standardDatasetSize))
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);

    List<Integer> facetString =
        new ArrayList(mapper.convertValue(facets.get("facetString"), Map.class).values());
    assertTrue(facetString.get(0) >= facetString.get(1));

    List<Integer> facetInteger =
        new ArrayList(mapper.convertValue(facets.get("facetInteger"), Map.class).values());
    assertTrue(facetInteger.get(0) >= facetInteger.get(1));

    List<Integer> facetBoolean =
        new ArrayList(mapper.convertValue(facets.get("facetBoolean"), Map.class).values());
    assertTrue(facetBoolean.get(0) >= facetBoolean.get(1));

    List<Integer> facetArray =
        new ArrayList(mapper.convertValue(facets.get("array_integer"), Map.class).values());
    assertTrue(facetArray.size() == standardDatasetSize);
    int index = 0;
    while ((index + 1) < standardDatasetSize) {
      assertTrue(facetArray.get(index) >= facetArray.get(index + 1));
      index++;
    }
  }

  @Test
  public void validateFacetKeyASCSort() throws IOException {
    String response =
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
                    + "?facets=facetString sortFacet: _key ASC, isEven sortFacet: _count DESC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"A\":18,\"B\":12}", facets.get("facetString").toString());
    assertEquals("{\"false\":15,\"true\":15}", facets.get("isEven").toString());
  }

  @Test
  public void validateNestedFacetKeyASCSort() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(TEST_DATA_INDEX + "?facets=nested.boolean sortFacet: _key ASC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"false\":15,\"true\":15}", facets.get("nested.boolean").toString());
  }

  @Test
  public void validateFacetKeyDESCSort() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(TEST_DATA_INDEX + "?facets=facetString sortFacet: _key DESC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"B\":12,\"A\":18}", facets.get("facetString").toString());
  }

  @Test
  public void validateNestedFacetKeyDESCSort() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(TEST_DATA_INDEX + "?facets=nested.boolean sortFacet: _key DESC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"true\":15,\"false\":15}", facets.get("nested.boolean").toString());
  }

  @Test
  public void validateFacetCountASCSort() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(TEST_DATA_INDEX + "?facets=facetString sortFacet: _count ASC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"B\":12,\"A\":18}", facets.get("facetString").toString());
  }

  @Test
  public void validateFacetCountDESCSort() throws IOException {
    String response =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get(TEST_DATA_INDEX + "?facets=facetString sortFacet: _count DESC")
            .body()
            .asString();

    JsonNode facets = mapper.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"A\":18,\"B\":12}", facets.get("facetString").toString());
  }
}
