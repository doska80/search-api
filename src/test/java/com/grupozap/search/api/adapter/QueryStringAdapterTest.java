package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.adapter.QueryStringAdapter.DEFAULT_BOOST_VALUE;
import static com.grupozap.search.api.adapter.QueryStringAdapter.DEFAULT_MAX_EXPANSIONS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.*;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldCacheFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.create;
import static org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.*;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import com.grupozap.search.api.adapter.QueryStringAdapter.QSTemplate;
import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.search.Queryable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.junit.Before;
import org.junit.Test;

public class QueryStringAdapterTest extends SearchTransportClientMock {

  private QueryStringAdapter queryStringAdapter;

  @Before
  public void setup() {
    QS_MM.setValue(DEFAULT_INDEX, "75%");
    QS_DEFAULT_FIELDS.setValue(INDEX_NAME, newArrayList("field", "field1"));
    QS_TEMPLATES.setValue(INDEX_NAME, null);

    queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
  }

  @Test
  public void shouldReturnSimpleSearchRequestByQueryString() {
    var q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              var boolQueryBuilder = boolQuery();
              Queryable queryable = request.q(q).build();

              queryStringAdapter.apply(boolQueryBuilder, queryable);
              assertEquals(1, boolQueryBuilder.must().size());
              assertTrue(boolQueryBuilder.should().isEmpty());
              var multiMatchQueryBuilder = (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());
              assertEquals(OR, multiMatchQueryBuilder.operator());
            });
  }

  @Test
  public void shouldReturnSimpleSearchRequestByQueryStringWithSpecifiedFieldToSearch() {
    var q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    var fieldName1 = "field1.keyword";
    var boostValue1 = 1.0f; // default boost value

    var fieldName2 = "field2";
    var boostValue2 = 2.0f;

    var fieldName3 = "field3";
    var boostValue3 = 5.0f;

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
              var boolQueryBuilder = boolQuery();
              queryStringAdapter.apply(boolQueryBuilder, request.q(q).fields(fields).build());
              assertEquals(1, boolQueryBuilder.must().size());
              assertTrue(boolQueryBuilder.should().isEmpty());
              var multiMatchQueryBuilder = (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());

              Map<String, Float> fieldsAndWeights = new HashMap<>(3);
              fieldsAndWeights.put(fieldName1, boostValue1);
              fieldsAndWeights.put(fieldName2, boostValue2);
              fieldsAndWeights.put(fieldName3, boostValue3);

              assertEquals(fieldsAndWeights, multiMatchQueryBuilder.fields());
            });
  }

  @Test
  public void shouldReturnSearchRequestByQueryStringWithValidMinimalShouldMatch() {
    var q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    List<String> validMMs = newArrayList("-100%", "100%", "75%", "-2");

    validMMs.forEach(
        mm ->
            newArrayList(filterableRequest, fullRequest)
                .parallelStream()
                .forEach(
                    request -> {
                      var boolQueryBuilder = boolQuery();
                      queryStringAdapter.apply(boolQueryBuilder, request.q(q).mm(mm).build());
                      assertEquals(1, boolQueryBuilder.must().size());
                      assertTrue(boolQueryBuilder.should().isEmpty());
                      var multiMatchQueryBuilder =
                          (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);

                      assertNotNull(multiMatchQueryBuilder);
                      assertEquals(q, multiMatchQueryBuilder.value());
                      assertEquals(mm, multiMatchQueryBuilder.minimumShouldMatch());
                      assertEquals(OR, multiMatchQueryBuilder.operator());
                    }));
  }

  @Test
  public void shouldApplyDefaultIndexFieldsForQuery() {
    var build = create().index(INDEX_NAME).q("any text to search").build();

    var boolQueryBuilder = boolQuery();
    queryStringAdapter.apply(boolQueryBuilder, build);

    Map<String, Float> expectedFields = new HashMap<>();
    expectedFields.put("field", 1f);
    expectedFields.put("field1", 1f);

    assertEquals(1, boolQueryBuilder.must().size());
    assertTrue(boolQueryBuilder.should().isEmpty());
    var multiMatchQueryBuilder = (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0);
    assertEquals(expectedFields, multiMatchQueryBuilder.fields());
  }

  @Test(expected = InvalidFieldException.class)
  public void shouldThrowExceptionWhenTryQueryOverInvalidField() {
    var build =
        create()
            .index(INDEX_NAME)
            .q("search")
            .fields(newHashSet("thisOneIsValid", "invalidField"))
            .build();
    queryStringAdapter.apply(boolQuery(), build);
  }

  @Test
  public void shouldUseIndexFieldsAliasForQuery() {
    var qsTemplate = new QSTemplate();
    qsTemplate.setFieldAliases(
        new HashMap<>() {
          {
            put("someFieldAlias", newArrayList("field1:8", "field2"));
          }
        });

    QS_TEMPLATES.setValue(INDEX_NAME, newArrayList(qsTemplate));
    queryStringAdapter.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, INDEX_NAME));

    var build =
        create()
            .index(INDEX_NAME)
            .q("search")
            .fields(newHashSet("someFieldAlias", "existingValidField"))
            .build();

    var boolQueryBuilder = boolQuery();
    queryStringAdapter.apply(boolQueryBuilder, build);

    assertEquals(1, boolQueryBuilder.must().size());
    assertTrue(boolQueryBuilder.should().isEmpty());

    // Validate multi match clause
    validateMultiMatchQueryBuilder(
        (MultiMatchQueryBuilder) boolQueryBuilder.must().get(0),
        BEST_FIELDS,
        DEFAULT_BOOST_VALUE,
        DEFAULT_MAX_EXPANSIONS,
        new HashMap<>() {
          {
            put("field1", 8f);
            put("field2", 1f);
            put("existingValidField", 1f);
          }
        });
  }

  @Test
  public void shouldHaveManyClausesInsideABoolQueryWhenUseMultipleQueryTemplate() {
    var firstTemplate = new QSTemplate();
    firstTemplate.setType(PHRASE_PREFIX.name());

    float boost = 4.0f;
    firstTemplate.setBoost(boost);

    int maxExpansions = 3;
    firstTemplate.setMaxExpansions(maxExpansions);

    firstTemplate.setFieldAliases(
        new HashMap<>() {
          {
            put("anotherAlias", newArrayList("field1:8", "field2:4", "field3:2"));
          }
        });

    var secondTemplate = new QSTemplate();
    secondTemplate.setType(CROSS_FIELDS.name());
    secondTemplate.setFieldAliases(
        new HashMap<>() {
          {
            put("anotherAlias", newArrayList("field2:3", "field3"));
          }
        });

    List<QSTemplate> templates = newArrayList(firstTemplate, secondTemplate, new QSTemplate());
    QS_TEMPLATES.setValue(INDEX_NAME, templates);
    queryStringAdapter.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, INDEX_NAME));

    var build =
        create()
            .index(INDEX_NAME)
            .q("search")
            .fields(newHashSet("anotherAlias", "existingValidField"))
            .build();

    var boolQueryBuilder = boolQuery();
    queryStringAdapter.apply(boolQueryBuilder, build);

    assertEquals(1, boolQueryBuilder.must().size());
    assertTrue(boolQueryBuilder.should().isEmpty());

    var boolQuery = (BoolQueryBuilder) boolQueryBuilder.must().get(0);
    assertEquals(templates.size(), boolQuery.should().size());

    // Validate first clause
    validateMultiMatchQueryBuilder(
        (MultiMatchQueryBuilder) boolQuery.should().get(0),
        PHRASE_PREFIX,
        boost,
        maxExpansions,
        new HashMap<>() {
          {
            put("field1", 8f);
            put("field2", 4f);
            put("field3", 2f);
            put("existingValidField", 1f);
          }
        });

    // Validate second clause
    validateMultiMatchQueryBuilder(
        (MultiMatchQueryBuilder) boolQuery.should().get(1),
        CROSS_FIELDS,
        DEFAULT_BOOST_VALUE,
        DEFAULT_MAX_EXPANSIONS,
        new HashMap<>() {
          {
            put("field2", 3f);
            put("field3", 1f);
            put("existingValidField", 1f);
          }
        });

    // Validate third clause
    validateMultiMatchQueryBuilder(
        (MultiMatchQueryBuilder) boolQuery.should().get(2),
        BEST_FIELDS,
        DEFAULT_BOOST_VALUE,
        DEFAULT_MAX_EXPANSIONS,
        new HashMap<>() {
          {
            put("anotherAlias", 1f);
            put("existingValidField", 1f);
          }
        });
  }

  private void validateMultiMatchQueryBuilder(
      MultiMatchQueryBuilder multiMatchQueryBuilder,
      Type expectedType,
      float expectedBoost,
      int expectedMaxExpansions,
      Map<String, Float> expectedFields) {
    assertEquals(expectedFields, multiMatchQueryBuilder.fields());
    assertEquals(expectedType, multiMatchQueryBuilder.type());
    assertEquals(expectedBoost, multiMatchQueryBuilder.boost(), 0.0);
    assertEquals(expectedMaxExpansions, multiMatchQueryBuilder.maxExpansions(), 0.0);
  }
}
