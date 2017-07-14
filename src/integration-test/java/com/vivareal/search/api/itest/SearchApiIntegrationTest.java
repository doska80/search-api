package com.vivareal.search.api.itest;

import com.vivareal.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.vivareal.search.api.itest.configuration.es.ESIndexHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.STANDARD_DATASET_SIZE;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties", "classpath:configuration/application-itest.properties"})
@ContextConfiguration(classes = SearchApiIntegrationTestContext.class)
public class SearchApiIntegrationTest {

    @BeforeClass
    public static void setup() {
        loadAll();
    }

    @Value("${search.api.base.url}")
    private String baseUrl;

    @Autowired
    private ESIndexHandler esIndexHandler;

    @Before
    public void clearTestData() throws IOException {
        esIndexHandler.truncateIndexData();
        esIndexHandler.addStandardTestData();
    }

    @Test
    public void responseOkWhenSearchAnExistingDocumentById() throws IOException {
        range(1, STANDARD_DATASET_SIZE + 1).boxed().forEach(id -> {
            given()
                .log().all()
                .baseUri(baseUrl)
                .contentType(JSON)
            .expect()
                .statusCode(SC_OK)
            .when()
                .get(TEST_DATA_INDEX + "/" + id)
            .then()
                .body("id", equalTo(String.valueOf(id)))
                .body("$", not(hasKey("nonExistingKey")))
                .body("numeric", equalTo(id))
                .body("array_integer", hasSize(id-1))
                .body("array_integer", equalTo(range(1, id).boxed().collect(toList())))
                .body("field" + id, equalTo("value" + id))
                .body("isEven", equalTo(id%2 == 0))
                .body("nested.boolean", equalTo(id%2 != 0))
                .body("nested.number", equalTo(id * 2))
                .body("nested.float", equalTo(id * 3.5f))
                .body("nested.string", equalTo(format("string_with_char(%s)", (char) (id + 'a'))))
                .body("nested.object.field", equalTo("common"))
                .body("nested.object.array_string", hasSize(id-1))
                .body("nested.object.array_string", equalTo(range(1, id).boxed().map(String::valueOf).collect(toList())));

        });
    }

    @Test
    public void responseOkWhenSearchAnExistingDocumentByIdWithIncludeAndExcludeFields() throws IOException {
        int id = STANDARD_DATASET_SIZE / 2;
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s/%s?includeFields=id,numeric&excludeFields=numeric", TEST_DATA_INDEX, id))
        .then()
            .body("id", equalTo(String.valueOf(id)))
            .body("numeric", equalTo(id))
            .body("$", not(hasKey("array_integer")))
            .body("$", not(hasKey("field" + id)))
            .body("$", not(hasKey("isEven")))
            .body("$", not(hasKey("nested")));
    }

    @Test
    public void responseBadRequestWhenSearchANonExistingIndex() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
        .get("/non-existing-index");
    }

    @Test
    public void responseNotFoundWhenSearchANonExistingDocumentById() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_NOT_FOUND)
        .when()
            .get(TEST_DATA_INDEX + "/" + STANDARD_DATASET_SIZE + 1);
    }

    @Test
    public void validateEqualsFilter() {
        int id = STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric EQ " + id)
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata", hasSize(1))
            .body("result.testdata[0].id", equalTo(String.valueOf(id)))
            .body("result.testdata[0].field" + id, equalTo("value" + id));
    }

    @Test
    public void validateEqualsFilterForStringDoubleQuoted() {
        int id = STANDARD_DATASET_SIZE / 2;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=field%d:\"value%d\"", TEST_DATA_INDEX, id, id))
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata", hasSize(1))
            .body("result.testdata[0].id", equalTo(String.valueOf(id)))
            .body("result.testdata[0].field" + id, equalTo("value" + id));
    }

    @Test
    public void validateEqualsFilterForStringSingleQuoted() {
        int id = STANDARD_DATASET_SIZE / 6;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=field%d:'value%d'", TEST_DATA_INDEX, id, id))
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata", hasSize(1))
            .body("result.testdata[0].id", equalTo(String.valueOf(id)))
            .body("result.testdata[0].field" + id, equalTo("value" + id));
    }

    @Test
    public void validateDifferentFilter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=isEven NE true")
        .then()
            .body("totalCount", equalTo(STANDARD_DATASET_SIZE / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(1, STANDARD_DATASET_SIZE).boxed().filter(id -> id % 2 != 0).collect(toList())));
    }

    @Test
    public void validateGreaterThanFilter() {
        int limit = STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric GT " + limit)
        .then()
            .body("totalCount", equalTo(STANDARD_DATASET_SIZE - limit))
            .body("result.testdata", hasSize(STANDARD_DATASET_SIZE - limit))
            .body("result.testdata.numeric.sort()", equalTo(range(limit + 1, STANDARD_DATASET_SIZE + 1).boxed().collect(toList())));
    }

    @Test
    public void validateGreaterOrEqualsThanFilter() {
        int limit = 2 * STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric GTE " + limit)
        .then()
            .body("totalCount", equalTo(STANDARD_DATASET_SIZE - limit + 1))
            .body("result.testdata", hasSize(STANDARD_DATASET_SIZE - limit + 1))
            .body("result.testdata.numeric.sort()", equalTo(range(limit, STANDARD_DATASET_SIZE + 1).boxed().collect(toList())));
    }

    @Test
    public void validateLowerThanFilter() {
        int limit = STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric LT " + limit)
        .then()
            .body("totalCount", equalTo(limit - 1))
            .body("result.testdata", hasSize(limit - 1))
            .body("result.testdata.numeric.sort()", equalTo(range(1, limit).boxed().collect(toList())));
    }

    @Test
    public void validateLowerOrEqualsThanFilter() {
        int limit = STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric LTE " + limit)
        .then()
            .body("totalCount", equalTo(limit))
            .body("result.testdata", hasSize(limit))
            .body("result.testdata.numeric.sort()", equalTo(range(1, limit + 1).boxed().collect(toList())));
    }

    @Test
    public void validateInFilterOnSingleField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric IN [1, " + STANDARD_DATASET_SIZE + "]")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata", hasSize(2))
            .body("result.testdata.numeric.sort()", equalTo(Stream.of(1, STANDARD_DATASET_SIZE).collect(toList())));
    }

    @Test
    public void validateInFilterOnArrayField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=nested.object.array_string IN [\"" + (STANDARD_DATASET_SIZE-2) + "\", \"" + (STANDARD_DATASET_SIZE - 1) + "\", \"" + STANDARD_DATASET_SIZE + "\"]")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata", hasSize(2))
            .body("result.testdata.numeric.sort()", equalTo(Stream.of(STANDARD_DATASET_SIZE-1, STANDARD_DATASET_SIZE).collect(toList())));
    }

    @Test
    public void validateViewportFilterOnGeoField() {
        int from = STANDARD_DATASET_SIZE / 3;
        int to = 2 * STANDARD_DATASET_SIZE / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f]", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from))
        .then()
            .body("totalCount", equalTo(to - from - 1))
            .body("result.testdata", hasSize(to - from - 1))
            .body("result.testdata.numeric.sort()", equalTo(range(from + 1, to).boxed().collect(toList())));
    }


    @Test
    public void validateLogicalOperatorAnd() {
        int from = 1;
        int to = STANDARD_DATASET_SIZE;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f] AND isEven EQ true", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from))
        .then()
            .body("totalCount", equalTo((to - from - 1) / 2))
            .body("result.testdata", hasSize((to - from - 1) / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(from + 1, to).boxed().filter(id -> id % 2 == 0).collect(toList())));
    }

    @Test
    public void validateLogicalOperatorOr() {
        int from = 1;
        int firstThird = STANDARD_DATASET_SIZE / 3;
        int secondThird = 2 * STANDARD_DATASET_SIZE / 3;
        int to = STANDARD_DATASET_SIZE;


        String filter1 = format("geo VIEWPORT [%.2f,%.2f;%.2f,%.2f]", from * -1f, (float) firstThird, firstThird * -1f, (float) from);
        String filter2 = format("geo VIEWPORT [%.2f,%.2f;%.2f,%.2f]", secondThird * -1f, (float) to, to * -1f, (float) secondThird);


        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=%s OR %s", TEST_DATA_INDEX, filter1, filter2))
        .then()
            .body("totalCount", equalTo((firstThird - from - 1) + (to - secondThird - 1)))
            .body("result.testdata", hasSize((firstThird - from - 1) + (to - secondThird - 1)))
            .body("result.testdata.numeric.sort()", equalTo(concat(range(from + 1, firstThird).boxed(), range(secondThird + 1, to).boxed()).collect(toList())));
    }

    @Test
    public void validateLogicalOperatorNot() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=NOT isEven EQ true")
        .then()
            .body("totalCount", equalTo(STANDARD_DATASET_SIZE / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(1, STANDARD_DATASET_SIZE).boxed().filter(id -> id % 2 != 0).collect(toList())));
    }

    @Test
    public void validateIsNullField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=nested.even=null")
        .then()
            .body("totalCount", equalTo(STANDARD_DATASET_SIZE / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(1, STANDARD_DATASET_SIZE).boxed().filter(id -> id % 2 != 0).collect(toList())));
    }
}
