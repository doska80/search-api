package com.grupozap.search.api.itest.scenarios;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.grupozap.search.api.itest.SearchApiIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RankFeatureQueryIntegrationTest extends SearchApiIntegrationTest {

  @Test
  public void shouldReturnTheResultOrderedByPriorityDescUsingTheRankFeatureQuery()
      throws InterruptedException {

    esIndexHandler.putStandardProperty("es.rfq", "priority:4");
    esIndexHandler.addStandardProperties();

    esIndexHandler.refreshIndex("");

    sleep(1000);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?disableSort=true&disableRfq=false")
        .then()
        .body(
            "result.testdata.priority",
            equalTo(
                range(1, standardDatasetSize)
                    .boxed()
                    .map(i -> standardDatasetSize - i + 1)
                    .limit(defaultPageSize)
                    .collect(toList())));
  }

  @Test
  public void shouldReturnTheResultOrderedByDefaultWhenRFQIsDisabled() throws InterruptedException {

    esIndexHandler.putStandardProperty("es.rfq", "priority:4");
    esIndexHandler.addStandardProperties();

    esIndexHandler.refreshIndex("");

    sleep(1000);

    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get(TEST_DATA_INDEX + "?disableSort=true&disableRfq=true")
        .then()
        .body(
            "result.testdata.priority",
            is(
                not(
                    range(1, standardDatasetSize)
                        .boxed()
                        .map(i -> standardDatasetSize - i + 1)
                        .limit(defaultPageSize)
                        .collect(toList()))));
  }
}
