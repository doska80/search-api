package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_EXCLUDES;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_INCLUDES;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.basic;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.search.Fetchable;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import java.util.HashSet;
import java.util.Set;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SourceFieldAdapterTest extends SearchTransportClientMock {

  private static final String SOME_ID = "123";

  @InjectMocks private SourceFieldAdapter sourceFieldAdapter;

  @Mock private FieldCache fieldCache;

  @Before
  public void setup() {
    initMocks(this);

    when(fieldCache.isIndexHasField(anyString(), contains("field"))).thenReturn(true);
    when(fieldCache.isIndexHasField(anyString(), contains("invalid")))
        .thenThrow(new InvalidFieldException("invalid", INDEX_NAME));

    SOURCE_INCLUDES.setValue(INDEX_NAME, null);
    SOURCE_EXCLUDES.setValue(INDEX_NAME, null);
  }

  @Test
  public void isValidWildCardForRequestById() {
    Fetchable fetchable =
        basic()
            .index(INDEX_NAME)
            .includeFields(newHashSet("*"))
            .excludeFields(new HashSet<>())
            .build();

    GetRequest getRequestBuilder = new GetRequest();
    sourceFieldAdapter.apply(getRequestBuilder, fetchable);

    FetchSourceContext fetchSourceContext = getRequestBuilder.fetchSourceContext();
    assertNotNull(fetchSourceContext);
    assertEquals(1, fetchSourceContext.includes().length);
    assertThat(fetchSourceContext.includes(), arrayContainingInAnyOrder("*"));
    assertThat(fetchSourceContext.excludes(), emptyArray());
  }

  @Test
  public void isValidWildCardForSearchRequest() {
    Fetchable fetchable =
        basic()
            .index(INDEX_NAME)
            .includeFields(newHashSet())
            .excludeFields(newHashSet("*"))
            .build();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, fetchable);

    FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
    assertNotNull(fetchSourceContext);

    assertThat(fetchSourceContext.includes(), emptyArray());
    assertEquals(1, fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("*"));
  }

  @Test
  public void applyDefaultFetchSourceFieldsWhenRequestEmptyForRequestById() {
    SOURCE_INCLUDES.setValue(
        INDEX_NAME, newArrayList("default.field1", "default.field2", "default.field3"));
    SOURCE_EXCLUDES.setValue(
        INDEX_NAME, newArrayList("default.field2", "default.field3", "default.field4"));

    sourceFieldAdapter.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, INDEX_NAME));
    GetRequest getRequestBuilder = new GetRequest();
    sourceFieldAdapter.apply(getRequestBuilder, basic().index(INDEX_NAME).build());

    FetchSourceContext fetchSourceContext = getRequestBuilder.fetchSourceContext();
    assertNotNull(fetchSourceContext);
    assertEquals(3, fetchSourceContext.includes().length);
    assertThat(
        fetchSourceContext.includes(),
        arrayContainingInAnyOrder("default.field1", "default.field2", "default.field3"));
    assertEquals(1, fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("default.field4"));
  }

  @Test
  public void applyDefaultFetchSourceFieldsWhenRequestEmptyForForSearchRequest() {
    SOURCE_INCLUDES.setValue(
        INDEX_NAME, newArrayList("default.field1", "default.field2", "default.field3"));
    SOURCE_EXCLUDES.setValue(
        INDEX_NAME, newArrayList("default.field2", "default.field3", "default.field4"));

    sourceFieldAdapter.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, INDEX_NAME));
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, basic().index(INDEX_NAME).build());

    FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
    assertNotNull(fetchSourceContext);
    assertEquals(3, fetchSourceContext.includes().length);
    assertThat(
        fetchSourceContext.includes(),
        arrayContainingInAnyOrder("default.field1", "default.field2", "default.field3"));
    assertEquals(1, fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("default.field4"));
  }

  @Test
  public void validateEmptyIncludeAndExcludeFieldsForRequestById() {
    Fetchable fetchable =
        basic()
            .index(INDEX_NAME)
            .includeFields(new HashSet<>())
            .excludeFields(new HashSet<>())
            .build();

    GetRequest getRequestBuilder = new GetRequest();
    sourceFieldAdapter.apply(getRequestBuilder, fetchable);

    FetchSourceContext fetchSourceContext = getRequestBuilder.fetchSourceContext();
    assertNotNull(fetchSourceContext);
    assertThat(fetchSourceContext.includes(), emptyArray());
    assertThat(fetchSourceContext.excludes(), emptyArray());
  }

  @Test
  public void validateEmptyIncludeAndExcludeFieldsForSearchRequest() {
    Fetchable fetchable =
        basic()
            .index(INDEX_NAME)
            .includeFields(new HashSet<>())
            .excludeFields(new HashSet<>())
            .build();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, fetchable);

    FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
    assertNotNull(fetchSourceContext);
    assertThat(fetchSourceContext.includes(), emptyArray());
    assertThat(fetchSourceContext.excludes(), emptyArray());
  }

  @Test
  public void shouldApplySpecifiedFieldSourcesForRequestById() {
    Set<String> includeFields = newHashSet("field1", "field2"),
        excludeFields = newHashSet("field3", "field4");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    GetRequest getRequestBuilder = new GetRequest();
    sourceFieldAdapter.apply(getRequestBuilder, fetchable);

    FetchSourceContext fetchSourceContext = getRequestBuilder.fetchSourceContext();
    assertNotNull(fetchSourceContext);
    assertEquals(includeFields.size(), fetchSourceContext.includes().length);
    assertThat(fetchSourceContext.includes(), arrayContainingInAnyOrder("field1", "field2"));

    assertEquals(excludeFields.size(), fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("field3", "field4"));
  }

  @Test
  public void shouldReturnApplySpecifiedFieldSourcesForSearchRequest() {
    Set<String> includeFields = newHashSet("field1", "field2");
    Set<String> excludeFields = newHashSet("field3", "field4");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, fetchable);

    FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
    assertNotNull(fetchSourceContext);

    assertNotNull(fetchSourceContext);
    assertEquals(includeFields.size(), fetchSourceContext.includes().length);
    assertThat(fetchSourceContext.includes(), arrayContainingInAnyOrder("field1", "field2"));

    assertEquals(excludeFields.size(), fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("field3", "field4"));
  }

  @Test
  public void shouldApplySpecifiedFieldSourcesForRequestByIdWithSameFieldsIntoIncludeAndExclude() {
    Set<String> includeFields = newHashSet("field1", "field2", "field3", "field5");
    Set<String> excludeFields = newHashSet("field3", "field4", "field5");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    GetRequest getRequestBuilder = new GetRequest();

    sourceFieldAdapter.apply(getRequestBuilder, fetchable);

    FetchSourceContext fetchSourceContext = getRequestBuilder.fetchSourceContext();
    assertNotNull(fetchSourceContext);
    assertEquals(4, fetchSourceContext.includes().length);
    assertThat(
        fetchSourceContext.includes(),
        arrayContainingInAnyOrder("field1", "field2", "field3", "field5"));

    assertEquals(1, fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("field4"));
  }

  @Test
  public void
      shouldReturnApplySpecifiedFieldSourcesForSearchRequestWithSameFieldsIntoIncludeAndExclude() {
    Set<String> includeFields = newHashSet("field1", "field2", "field3");
    Set<String> excludeFields = newHashSet("field3", "field4");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, fetchable);

    FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
    assertNotNull(fetchSourceContext);

    assertEquals(3, fetchSourceContext.includes().length);
    assertThat(
        fetchSourceContext.includes(), arrayContainingInAnyOrder("field1", "field2", "field3"));

    assertEquals(1, fetchSourceContext.excludes().length);
    assertThat(fetchSourceContext.excludes(), arrayContainingInAnyOrder("field4"));
  }

  @Test(expected = InvalidFieldException.class)
  public void shouldThrowInvalidFieldExceptionWhenInvalidFieldForRequestById() {
    Set<String> includeFields = newHashSet("field1", "field2", "invalid3", "field5");
    Set<String> excludeFields = newHashSet("field3", "field4", "field5");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    GetRequest getRequestBuilder = new GetRequest();
    sourceFieldAdapter.apply(getRequestBuilder, fetchable);
  }

  @Test(expected = InvalidFieldException.class)
  public void shouldThrowInvalidFieldExceptionWhenInvalidFieldForSearchRequest() {
    Set<String> includeFields = newHashSet("field1", "field2", "field3");
    Set<String> excludeFields = newHashSet("field3", "invalid4");

    Fetchable fetchable =
        basic().index(INDEX_NAME).includeFields(includeFields).excludeFields(excludeFields).build();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    sourceFieldAdapter.apply(searchSourceBuilder, fetchable);
  }
}
