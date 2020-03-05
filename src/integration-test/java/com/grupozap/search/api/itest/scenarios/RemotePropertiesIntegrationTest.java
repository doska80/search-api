package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.Thread.sleep;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertTrue;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RemotePropertiesIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void shouldGetAllPropertiesFromESIndex() throws InterruptedException {

    var documents = 100;
    var keys = new HashMap<String, String>();

    esIndexHandler.deleteIndex(SEARCH_API_PROPERTIES_INDEX);

    rangeClosed(1, documents)
        .boxed()
        .forEach(
            x -> {
              var key = "test-" + x;
              keys.put(key, key);
            });

    keys.forEach(
        (key, value) ->
            esIndexHandler.insertEntityByIndex(
                SEARCH_API_PROPERTIES_INDEX, key, "{\"alias\":\"" + value + "\"}"));

    esIndexHandler.refreshIndex(SEARCH_API_PROPERTIES_INDEX);

    sleep(1000);

    var keyResult =
        given()
            .log()
            .all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
            .when()
            .get("/properties/remote")
            .then()
            .extract()
            .jsonPath()
            .get();

    //noinspection unchecked,rawtypes
    assertTrue(((Map) keyResult).keySet().containsAll(keys.keySet()));
  }
}
