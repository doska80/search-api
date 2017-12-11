package com.vivareal.search.api.itest.scenarios;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class NestedIntegrationTest extends SearchApiIntegrationTest {

    @Test
    public void validateLogicalOperatorNot() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=nested.id>=10 AND nested.id<=20 AND NOT nested.even<>null")
        .then()
            .body("result.testdata.nested.id.sort()", equalTo(rangeClosed(10, 20).boxed().filter(id -> id % 2 != 0).collect(toList())))
            .body("result.testdata.nested.even", everyItem(isEmptyOrNullString()));
    }

    @Test
    public void validateComplexQueryUsingNestedAndNoNestedFieldsRecursively() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=object.object.field IN ['common'] AND facetString:'A' AND (nested.object.field:'common' AND nested.id>=10 AND nested.id<=20)")
        .then()
            .body("result.testdata.nested.id.sort()", equalTo(rangeClosed(10, 18).boxed().collect(toList())))
            .body("result.testdata.facetString", everyItem(is("A")))
            .body("result.testdata.object.object.field", everyItem(is("common")));
    }

    @Test
    public void validateFilterFromNestedQueriesIntoAndOutRecursive() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=nested.even:null AND array_integer:10 AND nested.id <=25 AND nested.object.field:'common' AND object.object.field:'common' AND (nested.string LIKE 'string with char W%' OR nested.string LIKE 'string with char Y%') AND nested.id >=20")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata.nested.even", everyItem(isEmptyOrNullString()))
            .body("result.testdata.nested.id.sort()", equalTo(newArrayList(23, 25)));
    }

    @Test
    public void shouldBeSameResultWithDifferentQueries() throws IOException {
        String query1 = "NOT (nested.string:'string with char i' OR nested.string:'string with char h')";
        String query2 = "NOT nested.string:'string with char i' AND NOT nested.string:'string with char h'";
        String query3 = "NOT nested.string:'string with char i' AND (NOT nested.string:'string with char h')";

        assertEquals(getResponse(query1), getResponse(query2));
        assertEquals(getResponse(query2), getResponse(query3));
    }

    private String getResponse(final String query) throws IOException {
        String response = given()
            .log().all()
                .baseUri(baseUrl)
                .contentType(JSON)
            .expect()
                .statusCode(SC_OK)
            .when()
                .get(format("%s?size=%d&sort=id ASC&filter=%s", TEST_DATA_INDEX, standardDatasetSize, query)).body().print()
            ;
        return mapper.readTree(response).get("result").asText();
    }
}
