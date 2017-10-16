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
public class SortIntegrationTest extends SearchApiIntegrationTest {

    @Test
    public void validateSortAsc() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?sort=numeric ASC")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", equalTo(rangeClosed(1, defaultPageSize).boxed().map(String::valueOf).map(String::valueOf).collect(toList())))
        ;
    }

    @Test
    public void validateSortDesc() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?sort=numeric DESC")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", equalTo(range(1, standardDatasetSize).boxed().map(i -> standardDatasetSize - i + 1).limit(defaultPageSize).map(String::valueOf).collect(toList())))
        ;
    }

    @Test
    public void validateMultipleTest() {
        List<String> even = rangeClosed(1, standardDatasetSize).boxed().filter(i -> i % 2 == 0).limit(defaultPageSize).map(String::valueOf).collect(toList());
        List<String> odd = rangeClosed(1, standardDatasetSize).boxed().filter(i -> i % 2 != 0).limit(defaultPageSize-even.size()).map(String::valueOf).collect(toList());
        List<String> expected = new ArrayList<>(defaultPageSize);
        expected.addAll(even);
        expected.addAll(odd);

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?sort=isEven DESC,numeric ASC")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", equalTo(expected))
        ;
    }

}
