package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_TYPE;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RankFeatureQueryIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void
      shouldReturnTheResultOrderedByPriorityDescUsingTheRankFeatureQueryWhenDefaultSortContainsPriorityOnFieldName()
          throws IOException {

    esIndexHandler.deleteIndex(SEARCH_API_PROPERTIES_INDEX);
    esIndexHandler.insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        TEST_DATA_TYPE,
        jsonFileUtils.getBoostrapConfig("/json/rfq_with_function_score.json"));
    esIndexHandler.refreshIndex(SEARCH_API_PROPERTIES_INDEX);

    given()
        .filter(new RequestLoggingFilter())
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=priority_sort")
        .then()
        .body(
            "result.testdata.priority_x",
            equalTo(
                range(0, standardDatasetSize)
                    .boxed()
                    .map(i -> i + 1)
                    .limit(defaultPageSize)
                    .collect(toList())));
  }

  @Test
  public void shouldReturnTheResultOrderedByPriorityDescUsingTheRankFeatureQuery()
      throws IOException {

    esIndexHandler.deleteIndex(SEARCH_API_PROPERTIES_INDEX);
    esIndexHandler.insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        TEST_DATA_TYPE,
        jsonFileUtils.getBoostrapConfig("/json/rfq_sort.json"));
    esIndexHandler.refreshIndex(SEARCH_API_PROPERTIES_INDEX);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=priority_sort")
        .then()
        .body(
            "result.testdata.priority_x",
            equalTo(
                range(1, standardDatasetSize)
                    .boxed()
                    .map(i -> standardDatasetSize - i + 1)
                    .limit(defaultPageSize)
                    .collect(toList())));
  }

  @Test
  public void shouldReturnTheResultOrderedByDefaultWhenRFQIsDisabled() throws IOException {

    esIndexHandler.deleteIndex(SEARCH_API_PROPERTIES_INDEX);
    esIndexHandler.insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        TEST_DATA_TYPE,
        jsonFileUtils.getBoostrapConfig("/json/rfq_sort.json"));
    esIndexHandler.refreshIndex(SEARCH_API_PROPERTIES_INDEX);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?sort=priority_sort&disableRfq=true")
        .then()
        .body(
            "result.testdata.priority_x",
            is(
                not(
                    range(1, standardDatasetSize)
                        .boxed()
                        .map(i -> standardDatasetSize - i + 1)
                        .limit(defaultPageSize)
                        .collect(toList()))));
  }
}
