package com.vivareal.search.api.itest;

import com.vivareal.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.vivareal.search.api.itest.configuration.es.ESIndexHandler;
import org.hamcrest.Matchers;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static java.lang.Math.ceil;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
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

    @Before
    public void clearTestData() throws IOException {
        esIndexHandler.truncateIndexData();
        esIndexHandler.addStandardTestData();
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
            .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f]", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from))
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
            .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f] AND isEven EQ true", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from))
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
            .get(format("%s?filter=object.special_string LIKE '* with special chars \\* and + %n and \\? of ? to search *'", TEST_DATA_INDEX))
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
            .get(format("%s?filter=nested.special_string LIKE '* with special chars \\* and + %n and \\? of ? to search *'", TEST_DATA_INDEX))
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
            .get(format("%s?filter=NOT object.special_string LIKE '* with special chars \\* and + %n and \\? of a to search *'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize - 1))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.object.special_string", everyItem(not(containsString("of a to search"))))
        ;
    }

    @Test
    public void validateSearchByWildcardQueryWithNotWithNested() {
        given()
            .log().all()
            .baseUri(baseUrl)
            .contentType(JSON)
        .expect()
            .statusCode(SC_OK)
        .when()
            .get(format("%s?filter=NOT nested.special_string LIKE '* with special chars \\* and + %n and \\? of a to search *'", TEST_DATA_INDEX))
        .then()
            .body("totalCount", equalTo(standardDatasetSize - 1))
            .body("result.testdata", hasSize(defaultPageSize))
            .body("result.testdata.nested.special_string", everyItem(not(containsString("of a to search"))))
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
        .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f] AND (numeric <= %d AND (isEven=true OR object.odd <> null))", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from, half))
        .then()
        .body("totalCount", equalTo(expected))
        .body("result.testdata", hasSize(expected))
        .body("result.testdata.numeric.sort()", equalTo(rangeClosed(from + 1, standardDatasetSize / 2).boxed().collect(toList())));
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
            .get(format("%s?filter=geo VIEWPORT [%.2f,%.2f;%.2f,%.2f] AND (numeric <= %d AND (isEven=true OR nested.odd <> null))", TEST_DATA_INDEX, from * -1f, (float) to, to * -1f, (float) from, half))
        .then()
            .body("totalCount", equalTo(expected))
            .body("result.testdata", hasSize(expected))
            .body("result.testdata.numeric.sort()", equalTo(rangeClosed(from + 1, standardDatasetSize / 2).boxed().collect(toList())));
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
}
