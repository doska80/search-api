package com.vivareal.search.api.itest.scenarios;

import com.vivareal.search.api.itest.SearchApiIntegrationTest;
import com.vivareal.search.api.itest.configuration.es.BulkESIndexHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.*;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.HttpStatus.SC_GATEWAY_TIMEOUT;

@Component
@RunWith(SpringRunner.class)
public class QueryTimeoutIntegrationTest extends SearchApiIntegrationTest {

    @Autowired
    private BulkESIndexHandler bulkESIndexHandler;

    @Override
    @PostConstruct
    public void setupIndexHandler() throws IOException {
        esIndexHandler.truncateIndexData(TEST_DATA_INDEX);
        esIndexHandler.truncateIndexData(SEARCH_API_PROPERTIES_INDEX);
        esIndexHandler.setDefaultProperties();
        esIndexHandler.addStandardProperties();
    }

    @Test
    public void validateQueryTimeout() throws InterruptedException {
        esIndexHandler.putStandardProperty("es.query.timeout.value", 1);
        esIndexHandler.putStandardProperty("es.query.timeout.unit", NANOSECONDS.name());
        esIndexHandler.addStandardProperties();

        sleep(1500);

        List<Map> sources = new ArrayList<>();

        rangeClosed(1, 2000)
            .boxed()
            .forEach(id -> {
                sources.add(esIndexHandler.createStandardEntityForId(id, false));
                if (sources.size() == 500) {
                    bulkESIndexHandler.bulkInsert(TEST_DATA_INDEX.replace("/", ""), TEST_DATA_TYPE, sources);
                    sources.clear();
                }
            });

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_GATEWAY_TIMEOUT)
        .when()
            .get(TEST_DATA_INDEX + "?facets=id,array_integer,numeric,nested.number,nested.boolean,nested.odd,nested.object.array_string")
        ;
    }
}
