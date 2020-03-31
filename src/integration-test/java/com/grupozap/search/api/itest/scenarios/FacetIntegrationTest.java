package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
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
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);

    List<Integer> facetString =
        new ArrayList(OBJECT_MAPPER.convertValue(facets.get("facetString"), Map.class).values());
    assertTrue(facetString.get(0) >= facetString.get(1));

    List<Integer> facetInteger =
        new ArrayList(OBJECT_MAPPER.convertValue(facets.get("facetInteger"), Map.class).values());
    assertTrue(facetInteger.get(0) >= facetInteger.get(1));

    List<Integer> facetBoolean =
        new ArrayList(OBJECT_MAPPER.convertValue(facets.get("facetBoolean"), Map.class).values());
    assertTrue(facetBoolean.get(0) >= facetBoolean.get(1));

    List<Integer> facetArray =
        new ArrayList(OBJECT_MAPPER.convertValue(facets.get("array_integer"), Map.class).values());
    assertEquals(facetArray.size(), (int) standardDatasetSize);
    var index = 0;
    while ((index + 1) < standardDatasetSize) {
      assertTrue(facetArray.get(index) >= facetArray.get(index + 1));
      index++;
    }
  }

  @Test
  public void validateFacetKeyASCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"A\":18,\"B\":12}", facets.get("facetString").toString());
    assertEquals("{\"false\":15,\"true\":15}", facets.get("isEven").toString());
  }

  @Test
  public void validateNestedFacetKeyASCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"false\":15,\"true\":15}", facets.get("nested.boolean").toString());
  }

  @Test
  public void validateFacetKeyDESCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"B\":12,\"A\":18}", facets.get("facetString").toString());
  }

  @Test
  public void validateNestedFacetKeyDESCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"true\":15,\"false\":15}", facets.get("nested.boolean").toString());
  }

  @Test
  public void validateFacetCountASCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"B\":12,\"A\":18}", facets.get("facetString").toString());
  }

  @Test
  public void validateFacetCountDESCSort() throws IOException {
    var response =
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

    var facets = OBJECT_MAPPER.readTree(response).get("result").get("facets");
    assertNotNull(facets);
    assertEquals("{\"A\":18,\"B\":12}", facets.get("facetString").toString());
  }

  @Test
  public void validateFacetFieldWithAlias() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?facets=field_before_alias")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata", everyItem(notNullValue()))
        .body("result.facets", notNullValue())
        .body(
            "result.facets.field_after_alias.findAll { it.value == 1 }.size()", equalTo(facetSize));
  }
}
