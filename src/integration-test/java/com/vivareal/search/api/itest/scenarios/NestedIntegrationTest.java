package com.vivareal.search.api.itest.scenarios;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class NestedIntegrationTest extends SearchApiIntegrationTest {

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
