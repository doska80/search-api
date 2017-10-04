package com.vivareal.search.api.itest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.vivareal.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.vivareal.search.api.itest.configuration.es.ESIndexHandler;
import org.apache.commons.lang3.StringUtils;
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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_TYPE;
import static java.lang.Math.ceil;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties", "classpath:configuration/application-itest.properties"})
@ContextConfiguration(classes = SearchApiIntegrationTestContext.class)
public class SearchApiIntegrationTest {

    @BeforeClass
    public static void setup() {
        loadAll();
    }

    @Value("${itest.standard.dataset.size}")
    private Integer standardDatasetSize;

    @Value("${es.facet.size}")
    private Integer facetSize;

    @Value("${search.api.base.url}")
    private String baseUrl;

    @Value("${es.default.size}")
    private Integer defaultPageSize;

    @Autowired
    private ESIndexHandler esIndexHandler;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void clearTestData() throws IOException {
        esIndexHandler.truncateIndexData(TEST_DATA_INDEX);
        esIndexHandler.addStandardTestData();

        esIndexHandler.truncateIndexData(SEARCH_API_PROPERTIES_INDEX);
        esIndexHandler.setDefaultProperties();
        esIndexHandler.addStandardProperties();
    }

    @Test
    public void responseOkWhenSearchAnExistingDocumentById() throws IOException {
        rangeClosed(1, standardDatasetSize).boxed().forEach(id ->
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
                .body("array_integer", hasSize(id))
                .body("array_integer", equalTo(rangeClosed(1, id).boxed().collect(toList())))
                .body("field" + id, equalTo("value" + id))
                .body("isEven", equalTo(id%2 == 0))
                .body("geo.lat", equalTo(-1f * id))
                .body("geo.lon", equalTo(id * 1f))

                .body("object.boolean", equalTo(id%2 != 0))
                .body("object.number", equalTo(id * 2))
                .body("object.float", equalTo(id * 3.5f))
                .body("object.string", equalTo(format("string with char %s", (char) (id + 'a' - 1))))
                .body("object.object.field", equalTo("common"))
                .body("object.object.array_string", hasSize(id))
                .body("object.object.array_string", equalTo(rangeClosed(1, id).boxed().map(String::valueOf).collect(toList())))

                .body("nested.boolean", equalTo(id%2 != 0))
                .body("nested.number", equalTo(id * 2))
                .body("nested.float", equalTo(id * 3.5f))
                .body("nested.string", equalTo(format("string with char %s", (char) (id + 'a' - 1))))
                .body("nested.object.field", equalTo("common"))
                .body("nested.object.array_string", hasSize(id))
                .body("nested.object.array_string", equalTo(rangeClosed(1, id).boxed().map(String::valueOf).collect(toList())))
        );
    }

    @Test
    public void responseOkWhenSearchAnExistingDocumentByIdWithIncludeAndExcludeFields() throws IOException {
        int id = standardDatasetSize / 2;
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
            .body("$", not(hasKey("object")))
            .body("$", not(hasKey("nested")))
            .body("$", not(hasKey("geo")));
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
            .get(TEST_DATA_INDEX + "/" + standardDatasetSize + 1);
    }

    @Test
    public void validateEqualsFilter() {
        int id = standardDatasetSize / 3;

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
        int id = standardDatasetSize / 2;

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
        int id = standardDatasetSize / 6;

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
            .body("totalCount", equalTo(standardDatasetSize / 2))
            .body("result.testdata", hasSize(standardDatasetSize / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(1, standardDatasetSize).boxed().filter(id -> id % 2 != 0).collect(toList())));
    }

    @Test
    public void validateGreaterThanFilter() {
        int limit = standardDatasetSize / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
            .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric GT " + limit)
        .then()
            .body("totalCount", equalTo(standardDatasetSize - limit))
            .body("result.testdata", hasSize(standardDatasetSize - limit))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(limit + 1, standardDatasetSize).boxed().collect(toList())));
    }

    @Test
    public void validateGreaterOrEqualsThanFilter() {
        int limit = 2 * standardDatasetSize / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?filter=numeric GTE " + limit)
        .then()
            .body("totalCount", equalTo(standardDatasetSize - limit + 1))
            .body("result.testdata", hasSize(standardDatasetSize - limit + 1))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(limit, standardDatasetSize).boxed().collect(toList())));
    }

    @Test
    public void validateLowerThanFilter() {
        int limit = standardDatasetSize / 3;

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
        int limit = standardDatasetSize / 3;

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
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, limit).boxed().collect(toList())));
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
            .get(TEST_DATA_INDEX + "?filter=numeric IN [1, " + standardDatasetSize + "]")
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata", hasSize(2))
            .body("result.testdata.numeric.sort()", equalTo(Stream.of(1, standardDatasetSize).collect(toList())));
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
            .body("result.testdata.numeric.sort()", equalTo(range));
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
            .body("result.testdata.numeric.sort()", equalTo(range));
    }

    @Test
    public void validateViewportFilterOnGeoField() {
        int from = standardDatasetSize / 3;
        int to = 2 * standardDatasetSize / 3;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]]", TEST_DATA_INDEX, (float) to, from * -1f, (float) from, to * -1f))
        .then()
            .body("totalCount", equalTo(to - from - 1))
            .body("result.testdata", hasSize(to - from - 1))
            .body("result.testdata.numeric.sort()", equalTo(range(from + 1, to).boxed().collect(toList())));
    }


    @Test
    public void validateLogicalOperatorAnd() {
        int from = 1;
        int to = standardDatasetSize;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]] AND isEven EQ true", TEST_DATA_INDEX, (float) to, from * -1f, (float) from, to * -1f))
        .then()
            .body("totalCount", equalTo((to - from - 1) / 2))
            .body("result.testdata", hasSize((to - from - 1) / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(from + 1, to).boxed().filter(id -> id % 2 == 0).collect(toList())));
    }

    @Test
    public void validateLogicalOperatorOr() {
        int from = 1;
        int firstThird = standardDatasetSize / 3;
        int secondThird = 2 * standardDatasetSize / 3;
        int to = standardDatasetSize;

        String filter1 = format("geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]]", (float) firstThird, from * -1f, (float) from, firstThird * -1f);
        String filter2 = format("geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]]", (float) to, secondThird * -1f, (float) secondThird, to * -1f);

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
            .body("totalCount", equalTo(standardDatasetSize / 2))
            .body("result.testdata", hasSize(standardDatasetSize / 2))
            .body("result.testdata.numeric.sort()", equalTo(range(1, standardDatasetSize).boxed().filter(id -> id % 2 != 0).collect(toList())));
    }

    @Test
    public void validateIsNullFieldWhenFilter() {
        Stream.of(TEST_DATA_INDEX + "?filter=object.even=null", TEST_DATA_INDEX + "?filter=NOT object.even<>null")
            .forEach(path ->
                given()
                    .log().all()
                    .baseUri(baseUrl)
                    .contentType(JSON)
                .expect()
                    .statusCode(SC_OK)
                .when()
                    .get(path)
                    .then()
                    .body("totalCount", equalTo(standardDatasetSize / 2))
                    .body("result.testdata", hasSize(standardDatasetSize / 2))
                    .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, standardDatasetSize).boxed().filter(id -> id % 2 != 0).collect(toList())))
            );
    }

    @Test
    public void validateIsNullFieldWhenFilterWhenNested() {
        Stream.of(TEST_DATA_INDEX + "?filter=nested.even=null", TEST_DATA_INDEX + "?filter=NOT nested.even<>null")
            .forEach(path ->
                given()
                    .log().all()
                    .baseUri(baseUrl)
                    .contentType(JSON)
                .expect()
                    .statusCode(SC_OK)
                .when()
                    .get(path)
                .then()
                    .body("totalCount", equalTo(standardDatasetSize / 2))
                    .body("result.testdata", hasSize(standardDatasetSize / 2))
                    .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, standardDatasetSize).boxed().filter(id -> id % 2 != 0).collect(toList())))
            );
    }

    @Test
    public void validateNotNullFieldWhenFilter() {
        Stream.of(TEST_DATA_INDEX + "?filter=object.even<>null", TEST_DATA_INDEX + "?filter=NOT object.even=null")
            .forEach(path ->
                given()
                    .log().all()
                    .baseUri(baseUrl)
                    .contentType(JSON)
                .expect()
                    .statusCode(SC_OK)
                .when()
                    .get(path)
                .then()
                    .body("totalCount", equalTo(standardDatasetSize / 2))
                    .body("result.testdata", hasSize(standardDatasetSize / 2))
                    .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, standardDatasetSize).boxed().filter(id -> id % 2 == 0).collect(toList())))
            );
    }

    @Test
    public void validateNotNullFieldWhenFilterWhenNested() {
        Stream.of(TEST_DATA_INDEX + "?filter=nested.even<>null", TEST_DATA_INDEX + "?filter=NOT nested.even=null")
            .forEach(path ->
                given()
                    .log().all()
                    .baseUri(baseUrl)
                    .contentType(JSON)
                .expect()
                    .statusCode(SC_OK)
                .when()
                    .get(path)
                .then()
                    .body("totalCount", equalTo(standardDatasetSize / 2))
                    .body("result.testdata", hasSize(standardDatasetSize / 2))
                    .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, standardDatasetSize).boxed().filter(id -> id % 2 == 0).collect(toList())))
            );
    }

    @Test
    public void validateSearchByWildcardQuery() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=object.special_string LIKE '%% with special chars \\* and + and %n and \\? and %% and 5%% and _ and with_underscore of _ to search %%'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.object.special_string", everyItem(containsString("with special chars")))
        ;
    }

    @Test
    public void validateSearchByWildcardQueryWhenNested() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=nested.special_string LIKE '%% with special chars \\* and + and %n and \\? and %% and 5%% and _ and with_underscore of _ to search %%'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.nested.special_string", everyItem(containsString("with special chars")))
        ;
    }

    @Test
    public void validateSearchByWildcardQueryWithNot() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=NOT object.special_string LIKE '%% with special chars \\* and + and %n and \\? and %% and 5%% and _ and with_underscore of a to search %%'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize - 1))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.object.special_string", everyItem(not(containsString("and _ and with_underscore of a to search"))))
        ;
    }

    @Test
    public void validateSearchByWildcardQueryWithNotWhenNested() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=NOT nested.special_string LIKE '%% with special chars \\* and + and %n and \\? and %% and 5%% and _ and with_underscore of a to search %%'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize - 1))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.nested.special_string", everyItem(not(containsString("and _ and with_underscore of a to search"))))
        ;
    }

    @Test
    public void validateIncludeFieldsWhenSearch() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?includeFields=id,geo")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", everyItem(notNullValue()))
            .body("result.testdata.numeric", everyItem(nullValue()))
            .body("result.testdata.array_integer", everyItem(nullValue()))
            .body("result.testdata.findAll { it.properties.findAll { it.key.startsWith('field') } }", empty())
            .body("result.testdata.isEven", everyItem(nullValue()))
            .body("result.testdata.object", everyItem(nullValue()))
            .body("result.testdata.nested", everyItem(nullValue()))
            .body("result.testdata.geo", everyItem(notNullValue()))
            .body("result.testdata.geo.lat", everyItem(notNullValue()))
            .body("result.testdata.geo.lon", everyItem(notNullValue()))
        ;
    }

    @Test
    public void validateExcludeFieldsWhenSearch() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?excludeFields=id,geo")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", everyItem(nullValue()))
            .body("result.testdata.numeric", everyItem(notNullValue()))
            .body("result.testdata.array_integer", everyItem(notNullValue()))
            .body("result.testdata.findAll { it.properties.findAll { it.key.startsWith('field') } }", everyItem(notNullValue()))
            .body("result.testdata.isEven", everyItem(notNullValue()))
            .body("result.testdata.geo", everyItem(nullValue()))

            .body("result.testdata.object", everyItem(notNullValue()))
            .body("result.testdata.object.boolean", everyItem(notNullValue()))
            .body("result.testdata.object.number", everyItem(notNullValue()))
            .body("result.testdata.object.float", everyItem(notNullValue()))
            .body("result.testdata.object.string", everyItem(notNullValue()))
            .body("result.testdata.object.object.field", everyItem(notNullValue()))
            .body("result.testdata.object.object.array_string", everyItem(notNullValue()))

            .body("result.testdata.nested", everyItem(notNullValue()))
            .body("result.testdata.nested.boolean", everyItem(notNullValue()))
            .body("result.testdata.nested.number", everyItem(notNullValue()))
            .body("result.testdata.nested.float", everyItem(notNullValue()))
            .body("result.testdata.nested.string", everyItem(notNullValue()))
            .body("result.testdata.nested.object.field", everyItem(notNullValue()))
            .body("result.testdata.nested.object.array_string", everyItem(notNullValue()))
        ;
    }

    @Test
    public void validateIncludeFieldsOverwritingExcludeFieldsWhenSearch() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?includeFields=id,geo&excludeFields=id,nested")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.id", everyItem(notNullValue()))
            .body("result.testdata.numeric", everyItem(nullValue()))
            .body("result.testdata.array_integer", everyItem(nullValue()))
            .body("result.testdata.findAll { it.properties.findAll { it.key.startsWith('field') } }", empty())
            .body("result.testdata.isEven", everyItem(nullValue()))
            .body("result.testdata.nested", everyItem(nullValue()))
            .body("result.testdata.geo", everyItem(notNullValue()))
            .body("result.testdata.geo.lat", everyItem(notNullValue()))
            .body("result.testdata.geo.lon", everyItem(notNullValue()))
        ;
    }

    @Test
    public void validateFacetFields() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?facets=id,array_integer,isEven")
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata", everyItem(notNullValue()))
            .body("result.facets", notNullValue())
            .body("result.facets.id.findAll { it.value == 1 }.size()", equalTo(facetSize))
            .body("result.facets.isEven.true", equalTo(standardDatasetSize / 2))
            .body("result.facets.isEven.false", equalTo(standardDatasetSize / 2))
            .body("result.facets.array_integer.findAll { it.key.toInteger() + it.value - 1 == " + standardDatasetSize + " }.size()", equalTo(facetSize))
        ;
    }

    @Test
    public void validateFacetSize() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?facets=id,array_integer&facetSize=" + standardDatasetSize)
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata", everyItem(notNullValue()))
            .body("result.facets", notNullValue())
            .body("result.facets.id.findAll { it.value == 1 }.size()", equalTo(standardDatasetSize))
            .body("result.facets.array_integer.findAll { it.key.toInteger() + it.value - 1 == " + standardDatasetSize + " }.size()", equalTo(standardDatasetSize))
        ;
    }

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

    @Test
    public void validatePaginationFromParameter() {
        int lastPage = (int) ceil(standardDatasetSize / (float) defaultPageSize);
        int lastPageSize = standardDatasetSize - defaultPageSize * (lastPage - 1);

        rangeClosed(1, lastPage).forEach(page -> {
            int from = defaultPageSize * (page - 1);
            given()
                .log().all()
                .baseUri(baseUrl)
                .contentType(JSON)
            .expect()
                .statusCode(SC_OK)
            .when()
                .get(TEST_DATA_INDEX + "?from=" + from)
            .then()
                .body("totalCount", equalTo(standardDatasetSize))
                .body("result.testdata", hasSize(page == lastPage ? lastPageSize : defaultPageSize))
                .body("result.testdata.id", everyItem(notNullValue()))
            ;
        });
    }

    @Test
    public void validateNegativePaginationFromParameter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(TEST_DATA_INDEX + "?from=-1")
        ;
    }

    @Test
    public void validatePaginationSizeParameter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX + "?size=" + standardDatasetSize)
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(standardDatasetSize))
            .body("result.testdata.id", everyItem(notNullValue()))
        ;
    }

    @Test
    public void validateNegativePaginationSizeParameter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(TEST_DATA_INDEX + "?size=-1")
        ;
    }

    @Test
    public void validateHighPaginationSizeParameter() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(TEST_DATA_INDEX + "?size=999999")
        ;
    }

    @Test
    public void validatePaginationFromAndSizeParameters() {
        int pageSize = standardDatasetSize / 7;
        int lastPage = (int) ceil(standardDatasetSize / (float) pageSize);
        int lastPageSize = standardDatasetSize - pageSize * (lastPage - 1);

        rangeClosed(1, lastPage).forEach(page -> {
            int from = pageSize * (page - 1);
            given()
                .log().all()
                .baseUri(baseUrl)
                .contentType(JSON)
            .expect()
                .statusCode(SC_OK)
            .when()
                .get(format("%s?from=%d&size=%d", TEST_DATA_INDEX, from, pageSize))
            .then()
                .body("totalCount", equalTo(standardDatasetSize))
                .body("result.testdata", hasSize(page == lastPage ? lastPageSize : pageSize))
                .body("result.testdata.id", everyItem(notNullValue()))
            ;
        });
    }

    @Test
    public void validateRecursiveFilter() {
        int from = standardDatasetSize / 3;
        int half = standardDatasetSize / 2;
        int to = 2 * standardDatasetSize / 3;
        int expected = standardDatasetSize / 6;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]] AND (numeric <= %d AND (isEven=true OR object.odd <> null))", TEST_DATA_INDEX, (float) to, from * -1f, (float) from, to * -1f, half))
        .then()
            .body("totalCount", equalTo(expected))
            .body("result.testdata", hasSize(expected))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(from + 1, standardDatasetSize / 2).boxed().collect(toList())))
        ;
    }

    @Test
    public void validateRecursiveFilterWhenNested() {
        int from = standardDatasetSize / 3;
        int half = standardDatasetSize / 2;
        int to = 2 * standardDatasetSize / 3;
        int expected = standardDatasetSize / 6;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo VIEWPORT [[%.2f,%.2f],[%.2f,%.2f]] AND (numeric <= %d AND (isEven=true OR nested.odd <> null))", TEST_DATA_INDEX, (float) to, from * -1f, (float) from, to * -1f, half))
        .then()
            .body("totalCount", equalTo(expected))
            .body("result.testdata", hasSize(expected))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(from + 1, standardDatasetSize / 2).boxed().collect(toList())))
        ;
    }

    @Test
    public void validateSearchByQInAllFields() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?q=string with char k", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata", everyItem(notNullValue()))
        ;
    }

    @Test
    public void validateSearchByQInInvalidField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?q=string with char&fields=object.numeric.raw", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchByQInInvalidFieldWhenNested() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?q=string with char&fields=nested.numeric.raw", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchByFilterWithNonExistingField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?filter=non_existing_field:1", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchWithNonExistingSort() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?sort=non_existing_field", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchWithNonExistingFacet() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?facets=non_existing_field", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchWithNonExistingIncludeField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?includeFields=non_existing_field", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchWithNonExistingExcludeField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?excludeFields=non_existing_field", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchByQInNonExistingField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?q=string with char&fields=non_existing_field", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchWithInvalidInjectedField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?size=a", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchByFilterWithInvalidField() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?filter=numeric:\"a\"", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchByQWithMM100Percent() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?q=string with char a&fields=object.string.raw&mm=100%%", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata", hasSize(1))
            .body("result.testdata[0].id", equalTo("1"))
        ;
    }

    @Test
    public void validateSearchByQWithMM100PercentWhenNested() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?q=string with char a&fields=nested.string.raw&mm=100%%", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata", hasSize(1))
            .body("result.testdata[0].id", equalTo("1"))
        ;
    }

    @Test
    public void validateClusterSettings() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get("/cluster/settings")
        .then()
            .body("testdata['index.number_of_shards']", equalTo("1"))
            .body("testdata['index.number_of_replicas']", equalTo("1"))
            .body("testdata['geo']", equalTo("geo_point"))
            .body("testdata['isEven']", equalTo("boolean"))
            .body("testdata['object']", equalTo("_obj"))
            .body("testdata['nested']", equalTo("nested"))
            .body("testdata['numeric']", equalTo("long"))
            .body("testdata['nested.number']", equalTo("long"))
            .body("testdata['nested.object.field.raw']", equalTo("text"))
            .body("testdata['object.object.array_string']", equalTo("keyword"))
        ;
    }

    @Test
    public void validateSearchNotFound() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_NOT_FOUND)
        .when()
            .get(format("%s/123456789", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateDefaultClusterProperties() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get("/properties/remote")
        .then()
            .body("testdata['es.default.size']", equalTo(20))
        ;
    }

    @Test
    public void validateUpdateClusterProperty() throws InterruptedException {
        int size = 10;
        esIndexHandler.putStandardProperty("es.default.size", size);
        esIndexHandler.addStandardProperties();

        sleep(1500);

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(TEST_DATA_INDEX)
        .then()
            .body("result.testdata", hasSize(size))
        ;
    }

    @Test
    public void openCircuit() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get("/forceOpen/true")
        ;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_INTERNAL_SERVER_ERROR)
        .when()
            .get(format("%s", TEST_DATA_INDEX))
        .then()
            .body("message", containsString("Hystrix circuit short-circuited and is OPEN"))
        ;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get("/forceOpen/false")
        ;

        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void hystrixStreamWorks() throws Exception {
        URL stream = new URL(baseUrl.replace("/v2", "") + "/application/hystrix.stream");
        InputStream in = stream.openStream();
        byte[] buffer = new byte[1024];
        in.read(buffer);
        String contents = new String(buffer);
        assertTrue("Wrong content: \n" + contents, contents.contains("data") || contents.contains("ping"));
        in.close();
    }


    @Test
    public void turbineStreamWorks() throws Exception {
        URL stream = new URL(baseUrl.replace("/v2", "") + "/turbine.stream");
        InputStream in = stream.openStream();
        byte[] buffer = new byte[1024];
        in.read(buffer);
        String contents = new String(buffer);
        assertTrue("Wrong content: \n" + contents, contents.contains("data") || contents.contains("ping"));
        in.close();
    }

    @Test
    public void searchStreamWorks() throws Exception {
        URL stream = new URL(format("%s%s/stream?includeFields=id", baseUrl, TEST_DATA_INDEX));
        InputStream in = stream.openStream();
        byte[] buffer = new byte[1024];
        in.read(buffer);
        String contents = new String(buffer);
        assertTrue("Wrong content: \n" + contents, contents.contains("id"));
        in.close();
    }

    @Test
    public void searchStreamSizeWorks() throws Exception {
        Object numberShards = given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .when()
            .get("/cluster/settings")
        .then()
            .extract()
            .path(format("%s['index.number_of_shards']", TEST_DATA_TYPE));

        URL stream = new URL(format("%s%s/stream?includeFields=id&size=1", baseUrl, TEST_DATA_INDEX));
        InputStream in = stream.openStream();
        byte[] buffer = new byte[1024];
        in.read(buffer);

        assertEquals(numberShards, StringUtils.countMatches(new String(buffer), '\n'));

        in.close();
    }

    @Test
    public void validateRecursionWithTwoFragmentLevelsUsingOR() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=(field10:\"value10\" AND isEven:true) OR (field9:\"value9\" AND isEven:false)", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata[0].id", equalTo("10"))
            .body("result.testdata[1].id", equalTo("9"))
        ;
    }

    @Test
    public void validateRecursionWithTwoFragmentLevelsUsingAND() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=(field10:\"value10\" AND isEven:true) AND (numeric:10 AND nested.boolean:false)", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(1))
            .body("result.testdata[0].id", equalTo("10"))
        ;
    }

    @Test
    public void validateRecursionWithTwoFragmentLevelsUsingNOT() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=NOT((field10:\"value10\" AND isEven:true) OR (field9:\"value9\" AND isEven:false))", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize - 2))
        ;
    }

    @Test
    public void validateResponseWithSizeZero() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?size=0", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize))
            .body("result.testdata", hasSize(0))
        ;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateFacetSortPosition() throws IOException {
        String response = given()
                .log()
                .all()
                .baseUri(baseUrl)
                .contentType(JSON)
            .expect()
                .statusCode(SC_OK)
            .when()
                .get(format("%s?size=0&facets=facetString,facetInteger,facetBoolean,array_integer&facetSize=%s", TEST_DATA_INDEX, standardDatasetSize)).body().asString();

        JsonNode facets = mapper.readTree(response).get("result").get("facets");
        assertNotNull(facets);

        List<Integer> facetString = new ArrayList(mapper.convertValue(facets.get("facetString"), Map.class).values());
        assertTrue(facetString.get(0) >= facetString.get(1));

        List<Integer> facetInteger = new ArrayList(mapper.convertValue(facets.get("facetInteger"), Map.class).values());
        assertTrue(facetInteger.get(0) >= facetInteger.get(1));

        List<Integer> facetBoolean = new ArrayList(mapper.convertValue(facets.get("facetBoolean"), Map.class).values());
        assertTrue(facetBoolean.get(0) >= facetBoolean.get(1));

        List<Integer> facetArray = new ArrayList(mapper.convertValue(facets.get("array_integer"), Map.class).values());
        assertTrue(facetArray.size() == standardDatasetSize);
        int index = 0;
        while ((index + 1) < standardDatasetSize) {
            assertTrue(facetArray.get(index) >= facetArray.get(index + 1));
            index++;
        }
    }

    @Test
    public void validateSearchUsingRangeOperator() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=numeric RANGE [1,5]", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(5))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, 5).boxed().collect(toList())))
        ;
    }

    @Test
    public void validateSearchUsingRangeOperatorWhenNot() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=NOT numeric RANGE [1,5]&size=%d", TEST_DATA_INDEX, standardDatasetSize))
        .then()
            .body("totalCount", equalTo(25))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(6, standardDatasetSize).boxed().collect(toList())))
        ;
    }

    @Test
    public void validateSearchUsingInvalidRangeOperator() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?filter=NOT numeric RANGE [1]", TEST_DATA_INDEX))
        ;
    }

    @Test
    public void validateSearchUsingPolygon() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=geo POLYGON [[0.0,0.0],[0.0,3.0],[3.0,-3.0],[-3.0,0.0]]", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(2))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(1, 2).boxed().collect(toList())))
        ;
    }

    @Test
    public void validateSearchUsingPolygonWithLessThan3Points() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_BAD_REQUEST)
        .when()
            .get(format("%s?filter=geo POLYGON [[0.0,0.0],[0.0,3.0]]", TEST_DATA_INDEX))
        ;
    }
}
