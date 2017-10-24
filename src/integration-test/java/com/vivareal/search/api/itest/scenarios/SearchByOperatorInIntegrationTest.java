package com.vivareal.search.api.itest.scenarios;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.of;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringRunner.class)
public class SearchByOperatorInIntegrationTest extends SearchApiIntegrationTest {

    @Test
    public void validateInFilterOnSingleField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric IN [1, " + standardDatasetSize + "]")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata", hasSize(2))
            .body("result.testdata.numeric.sort()", equalTo(of(1, standardDatasetSize).collect(toList())))
        ;
    }

    @Test
    public void validateInFilterOnArrayField() {
        List<Integer> range = rangeClosed(standardDatasetSize - 3, standardDatasetSize).boxed().collect(toList());

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=object.object.array_string IN [" + range.stream().map(i -> format("\"%d\"", i)).collect(joining(",")) + "]")
        .then()
            .body("totalCount", equalTo(range.size()))
            .body("result.testdata", hasSize(range.size()))
            .body("result.testdata.numeric.sort()", equalTo(range))
        ;
    }

    @Test
    public void validateInFilterOnArrayFieldWhenNested() {
        List<Integer> range = rangeClosed(standardDatasetSize - 3, standardDatasetSize).boxed().collect(toList());

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=nested.object.array_string IN [" + range.stream().map(i -> format("\"%d\"", i)).collect(joining(",")) + "]")
        .then()
            .body("totalCount", equalTo(range.size()))
            .body("result.testdata", hasSize(range.size()))
            .body("result.testdata.numeric.sort()", equalTo(range))
        ;
    }

    @Test
    public void validateSearchByIdsUsingInOperator() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?sort=id ASC&includeFields=id&filter=id IN [1, " + standardDatasetSize + "]")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata", hasSize(2))
            .body("result.testdata.id", equalTo(of("1", valueOf(standardDatasetSize)).collect(toList())))
        ;
    }

}
