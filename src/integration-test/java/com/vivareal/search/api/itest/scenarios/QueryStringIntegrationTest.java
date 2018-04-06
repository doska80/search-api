package com.vivareal.search.api.itest.scenarios;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.Thread.sleep;
import static org.apache.http.HttpStatus.SC_OK;
import static org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.PHRASE_PREFIX;
import static org.hamcrest.Matchers.equalTo;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class QueryStringIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void shouldReturnOneResultWhenUsingQParameter() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?q='string with char a'&fields=object.string_text&mm=100%")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata.id.get(0)", equalTo("1"));
  }

  @Test
  public void shouldReturnOneResultWhenUsingQParameterUsingMultipleQueryTemplate()
      throws InterruptedException {
    Map<String, List<String>> fieldAliases = new HashMap<>();
    fieldAliases.put("qs_alias", newArrayList("object.string_text:2"));
    fieldAliases.put(
        "another_qs_alias", newArrayList("object.string_text", "object.special_string"));

    Map<String, Object> firstTemplate = new HashMap<>();
    firstTemplate.put("type", PHRASE_PREFIX.name());
    firstTemplate.put("boost", 4);
    firstTemplate.put("fieldAliases", fieldAliases);

    Map<String, Object> secondTemplate = new HashMap<>();
    secondTemplate.put("fieldAliases", fieldAliases);

    esIndexHandler.putStandardProperty(
        "querystring.templates", newArrayList(firstTemplate, secondTemplate));
    esIndexHandler.addStandardProperties();

    sleep(1500);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?q='string with char a'&fields=qs_alias&mm=100%")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata.id.get(0)", equalTo("1"));
  }

  @Test
  public void shouldReturnOneResultWhenCombineQParameterWithValidFilter() {
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
                + "?q='string with char i'&fields=object.string_text&filter=numeric < 10&mm=100%")
        .then()
        .body("totalCount", equalTo(1))
        .body("result.testdata.id.get(0)", equalTo("9"));
  }

  @Test
  public void shouldReturnZeroResultWhenCombineQParameterWithInvalidFilter() {
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
                + "?q='string with char i'&fields=object.string&filter=numeric < 5&mm=100%")
        .then()
        .body("totalCount", equalTo(0));
  }
}
