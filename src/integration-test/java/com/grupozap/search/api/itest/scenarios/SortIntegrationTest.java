package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.data.TestData.latitude;
import static com.grupozap.search.api.itest.configuration.data.TestData.longitude;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SortIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void validateSortAsc() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=numeric ASC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body(
            "result.testdata.id",
            equalTo(
                rangeClosed(1, defaultPageSize).boxed().map(String::valueOf).collect(toList())));
  }

  @Test
  public void validateSortDesc() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=numeric DESC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body(
            "result.testdata.id",
            equalTo(
                range(1, standardDatasetSize)
                    .boxed()
                    .map(i -> standardDatasetSize - i + 1)
                    .limit(defaultPageSize)
                    .map(String::valueOf)
                    .collect(toList())));
  }

  @Test
  public void validateMultipleTest() {
    List<String> even =
        rangeClosed(1, standardDatasetSize)
            .boxed()
            .filter(i -> i % 2 == 0)
            .limit(defaultPageSize)
            .map(String::valueOf)
            .collect(toList());
    List<String> odd =
        rangeClosed(1, standardDatasetSize)
            .boxed()
            .filter(i -> i % 2 != 0)
            .limit(defaultPageSize - even.size())
            .map(String::valueOf)
            .collect(toList());
    List<String> expected = new ArrayList<>(defaultPageSize);
    expected.addAll(even);
    expected.addAll(odd);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=isEven DESC,numeric ASC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata.id", equalTo(expected));
  }

  @Test
  public void validateSortAscWhenNestedType() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?includeFields=nested.id&sort=nested.id ASC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body(
            "result.testdata.nested.id",
            equalTo(rangeClosed(1, defaultPageSize).boxed().collect(toList())));
  }

  @Test
  public void validateSortNestedArray() {
    ArrayList<ArrayList<HashMap>> nestedArray =
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
                    + "?includeFields=nested_array&filter=nested_array.number RANGE [100, 3000]&sort=nested_array.number ASC sortFilter:nested_array.string:\"b\"")
            .then()
            .extract()
            .path("result.testdata.nested_array");

    Integer lastMin = Integer.MIN_VALUE;

    for (ArrayList<HashMap> objectArray : nestedArray) {
      Integer currentMin = Integer.MAX_VALUE;
      for (HashMap object : objectArray) {
        if (!"b".equals(object.get("string"))) continue;

        Integer number = (Integer) object.get("number");
        if (number < currentMin) currentMin = number;
      }

      if (currentMin != Integer.MAX_VALUE) {
        assertTrue(lastMin < currentMin);
        lastMin = currentMin;
      }
    }
  }

  @Test
  public void validateSortNestedArrayRespectingRange() {
    LinkedList<ArrayList<HashMap>> nestedArray =
        new LinkedList<>(
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
                        + "?includeFields=nested_array&filter=nested_array.number RANGE [50, 5000]&sort=nested_array.number ASC sortFilter:nested_array.string:\"b\" AND nested_array.number RANGE [50, 5000]")
                .then()
                .extract()
                .path("result.testdata.nested_array"));

    Integer lastMin = Integer.MIN_VALUE;

    ArrayList<HashMap> objectArray;

    /*
        - first: the numbers from the "b" object type must be in ASC order and respect the range
    */
    firstSort:
    while (!nestedArray.isEmpty()) {
      objectArray = nestedArray.removeFirst();

      Integer currentMin = Integer.MAX_VALUE;

      for (HashMap object : objectArray) {
        if (!"b".equals(object.get("string"))) continue;

        Integer number = (Integer) object.get("number");

        if (number >= 50 && number <= 5000) {
          if (number < currentMin) currentMin = number;
        } else {
          break firstSort;
        }
      }

      if (currentMin != Integer.MAX_VALUE) {
        assertTrue(lastMin < currentMin);
        lastMin = currentMin;
      }
    }
  }

  @Test
  public void validateSortByProximity() {
    List<Float> expectedLat = new ArrayList<>();
    List<Float> expectedLon = new ArrayList<>();
    for (int i = standardDatasetSize; i > standardDatasetSize - defaultPageSize; i--) {
      expectedLat.add(latitude(i));
      expectedLon.add(longitude(i));
    }

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(format("%s?sort=geo NEAR [30.0, -30.0]", TEST_DATA_INDEX))
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata.geo.lat", equalTo(expectedLat))
        .body("result.testdata.geo.lon", equalTo(expectedLon));
  }

  @Test
  public void validateSortByProximitySameDistance() {
    int testValueLatitudeLongitude = 15;

    List<Float> expectedLat = new ArrayList<>();
    List<Float> expectedLon = new ArrayList<>();

    for (int i = 0;
        expectedLat.size() < defaultPageSize && expectedLon.size() < defaultPageSize;
        i++) {

      if (i == 0) {
        expectedLon.add(longitude(testValueLatitudeLongitude));
        expectedLat.add(latitude(testValueLatitudeLongitude));
      } else {
        expectedLon.add(longitude(testValueLatitudeLongitude) + i);
        expectedLat.add(latitude(testValueLatitudeLongitude) - i);

        expectedLon.add(longitude(testValueLatitudeLongitude) - i);
        expectedLat.add(latitude(testValueLatitudeLongitude) + i);
      }
    }

    expectedLat = expectedLat.subList(0, defaultPageSize);
    expectedLon = expectedLon.subList(0, defaultPageSize);

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
                "%s?sort=geo NEAR [%s, %s]",
                TEST_DATA_INDEX,
                longitude(testValueLatitudeLongitude),
                latitude(testValueLatitudeLongitude)))
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata.geo.lat", equalTo(expectedLat))
        .body("result.testdata.geo.lon", equalTo(expectedLon));
  }

  @Test
  public void validateSortByProximityWithSortFilter() {
    List<Float> expectedLat = new ArrayList<>();
    List<Float> expectedLon = new ArrayList<>();
    for (int i = standardDatasetSize; i > standardDatasetSize - defaultPageSize; i--) {
      expectedLat.add(latitude(i));
      expectedLon.add(longitude(i));
    }

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
                "%s?sort=geo NEAR [30.0, -30.0] sortFilter:nested_array.string EQ \"b\"",
                TEST_DATA_INDEX))
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body("result.testdata.geo.lat", equalTo(expectedLat))
        .body("result.testdata.geo.lon", equalTo(expectedLon));
  }

  @Test
  public void validateSortByProximityWithWrongOperator() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(format("%s?sort=geo FAR [30.0, -30.0]", TEST_DATA_INDEX));
  }

  @Test
  public void validateSortByProximityWithTooManyPoints() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(format("%s?sort=geo NEAR [30.0, -30.0, -25.0]", TEST_DATA_INDEX));
  }

  @Test
  public void validateSortByProximityWithFewerPoints() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST)
        .when()
        .get(format("%s?sort=geo NEAR [30.0]", TEST_DATA_INDEX));
  }

  @Test
  public void validateScriptSortASC() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=testdata_numericsort ASC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body(
            "result.testdata.id",
            equalTo(
                rangeClosed(1, defaultPageSize).boxed().map(String::valueOf).collect(toList())));
  }

  @Test
  public void validateScriptSortDESC() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=testdata_numericsort DESC")
        .then()
        .body("totalCount", equalTo(standardDatasetSize))
        .body("result.testdata", hasSize(defaultPageSize))
        .body(
            "result.testdata.id",
            equalTo(
                range(1, standardDatasetSize)
                    .boxed()
                    .map(i -> standardDatasetSize - i + 1)
                    .limit(defaultPageSize)
                    .map(String::valueOf)
                    .collect(toList())));
  }
}
