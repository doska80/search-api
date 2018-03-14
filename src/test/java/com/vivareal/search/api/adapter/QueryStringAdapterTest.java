package com.vivareal.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldCacheFixture;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.create;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.search.Queryable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.junit.Before;
import org.junit.Test;

public class QueryStringAdapterTest extends SearchTransportClientMock {

  private QueryStringAdapter queryStringAdapter;

  @Before
  public void setup() {
    QS_MM.setValue(DEFAULT_INDEX, "75%");
    QS_DEFAULT_FIELDS.setValue(INDEX_NAME, "field,field1");
    QS_ALIAS_FIELDS.setValue(INDEX_NAME, null);

    queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
  }

  @Test
  public void shouldReturnSimpleSearchRequestBuilderByQueryString() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              BoolQueryBuilder boolQueryBuilder = boolQuery();
              Queryable queryable = request.q(q).build();

              queryStringAdapter.apply(boolQueryBuilder, queryable);
              MultiMatchQueryBuilder multiMatchQueryBuilder =
                  (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());
              assertEquals(OR, multiMatchQueryBuilder.operator());
            });
  }

  @Test
  public void shouldReturnSimpleSearchRequestBuilderByQueryStringWithSpecifiedFieldToSearch() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    String fieldName1 = "field1.keyword";
    float boostValue1 = 1.0f; // default boost value

    String fieldName2 = "field2";
    float boostValue2 = 2.0f;

    String fieldName3 = "field3";
    float boostValue3 = 5.0f;

    Set<String> fields =
        Sets.newLinkedHashSet(
            newArrayList(
                String.format("%s", fieldName1),
                String.format("%s:%s", fieldName2, boostValue2),
                String.format("%s:%s", fieldName3, boostValue3)));

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              BoolQueryBuilder boolQueryBuilder = boolQuery();
              queryStringAdapter.apply(boolQueryBuilder, request.q(q).fields(fields).build());
              MultiMatchQueryBuilder multiMatchQueryBuilder =
                  (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());

              Map<String, Float> fieldsAndWeights = new HashMap<>(3);
              fieldsAndWeights.put(fieldName1, boostValue1);
              fieldsAndWeights.put(fieldName2, boostValue2);
              fieldsAndWeights.put(fieldName3, boostValue3);

              assertTrue(fieldsAndWeights.equals(multiMatchQueryBuilder.fields()));
            });
  }

  @Test
  public void shouldReturnSearchRequestBuilderByQueryStringWithValidMinimalShouldMatch() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    List<String> validMMs = Lists.newArrayList("-100%", "100%", "75%", "-2");

    validMMs.forEach(
        mm ->
            newArrayList(filterableRequest, fullRequest)
                .parallelStream()
                .forEach(
                    request -> {
                      BoolQueryBuilder boolQueryBuilder = boolQuery();
                      queryStringAdapter.apply(boolQueryBuilder, request.q(q).mm(mm).build());
                      MultiMatchQueryBuilder multiMatchQueryBuilder =
                          (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

                      assertNotNull(multiMatchQueryBuilder);
                      assertEquals(q, multiMatchQueryBuilder.value());
                      assertEquals(mm, multiMatchQueryBuilder.minimumShouldMatch());
                      assertEquals(OR, multiMatchQueryBuilder.operator());
                    }));
  }

  @Test
  public void shouldApplyDefaultIndexFieldsForQuery() {
    SearchApiRequest build = create().index(INDEX_NAME).q("any text to search").build();

    BoolQueryBuilder boolQueryBuilder = boolQuery();
    queryStringAdapter.apply(boolQueryBuilder, build);

    Map<String, Float> expectedFields = new HashMap<>();
    expectedFields.put("field", 1f);
    expectedFields.put("field1", 1f);

    MultiMatchQueryBuilder multiMatchQueryBuilder =
        (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);
    assertEquals(expectedFields, multiMatchQueryBuilder.fields());
  }

  @Test(expected = InvalidFieldException.class)
  public void shouldThrowExceptionWhenTryQueryOverInvalidField() {
    SearchApiRequest build =
        create()
            .index(INDEX_NAME)
            .q("search")
            .fields(newHashSet("thisOneIsValid", "invalidField"))
            .build();
    queryStringAdapter.apply(boolQuery(), build);
  }

  @Test
  public void shouldUseIndexFieldsAliasForQuery() {
    QS_ALIAS_FIELDS.setValue(INDEX_NAME, "someFieldAlias|field1:8,field2");
    queryStringAdapter.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, INDEX_NAME));

    SearchApiRequest build =
        create()
            .index(INDEX_NAME)
            .q("search")
            .fields(newHashSet("someFieldAlias", "existingValidField"))
            .build();

    BoolQueryBuilder boolQueryBuilder = boolQuery();
    queryStringAdapter.apply(boolQueryBuilder, build);

    Map<String, Float> expectedFields = new HashMap<>();
    expectedFields.put("field1", 8f);
    expectedFields.put("field2", 1f);
    expectedFields.put("existingValidField", 1f);

    MultiMatchQueryBuilder multiMatchQueryBuilder =
        (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);
    assertEquals(expectedFields, multiMatchQueryBuilder.fields());
  }
}
