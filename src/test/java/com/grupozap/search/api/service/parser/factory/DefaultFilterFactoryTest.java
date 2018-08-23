package com.grupozap.search.api.service.parser.factory;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.FILTER_DEFAULT_CLAUSES;
import static com.grupozap.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupozap.search.api.adapter.FilterQueryAdapter;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.query.Filter;
import com.grupozap.search.api.model.query.QueryFragmentItem;
import com.grupozap.search.api.model.query.QueryFragmentList;
import java.util.HashSet;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultFilterFactoryTest {

  private static final String SOME_INDEX = "index";

  private DefaultFilterFactory defaultFilterFactory;

  private QueryParser queryParser;
  private FilterQueryAdapter filterQueryAdapter;

  @BeforeClass
  public static void setUpClass() {
    loadAll();
  }

  @Before
  public void setup() {
    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, null);

    queryParser = mock(QueryParser.class);
    filterQueryAdapter = mock(FilterQueryAdapter.class);

    defaultFilterFactory = new DefaultFilterFactory(queryParser, filterQueryAdapter);
  }

  @Test
  public void emptyDefaultFiltersForIndexWithNullConfig() {
    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, null);
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));
    assertEquals(
        new HashSet<>(), defaultFilterFactory.getDefaultFilters(SOME_INDEX, new HashSet<>()));
  }

  @Test
  public void mustReturnSingleDefaultFilterForIndexWhenRequestDoesNotMatchFilterField() {
    QueryFragmentItem qfi = from(QueryFragmentItem.class).gimme("qfi");
    Filter filter = qfi.getFilter();

    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, newArrayList(filter.toString()));

    BoolQueryBuilder expectedBoolQuery = boolQuery();
    when(queryParser.parse(filter.toString())).thenReturn(qfi);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfi)).thenReturn(expectedBoolQuery);

    // Simulate DefaultFilters creation
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));

    // Must return default filter when request filter fields is empty
    assertEquals(
        singleton(expectedBoolQuery),
        defaultFilterFactory.getDefaultFilters(SOME_INDEX, new HashSet<>()));

    // Must return default filter, when request filter fields dont match any default filter field
    assertEquals(
        singleton(expectedBoolQuery),
        defaultFilterFactory.getDefaultFilters(
            SOME_INDEX, newHashSet(asList("non", "match", "any", "defaultFilterField"))));
  }

  @Test
  public void mustReturnMultipleDefaultFilterForIndexWhenRequestDoesNotMatchFilterField() {
    QueryFragmentItem qfi = from(QueryFragmentItem.class).gimme("qfi");
    QueryFragmentList qfiNested =
        new QueryFragmentList(
            newArrayList(
                from(QueryFragmentItem.class).gimme("qfi"),
                from(QueryFragmentItem.class).gimme("qfiNested")));

    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, newArrayList(qfi.toString(), qfiNested.toString()));

    BoolQueryBuilder expectedDefaultFilter1 = boolQuery();
    when(queryParser.parse(qfi.toString())).thenReturn(qfi);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfi)).thenReturn(expectedDefaultFilter1);

    BoolQueryBuilder expectedDefaultFilter2 = boolQuery();
    when(queryParser.parse(qfiNested.toString())).thenReturn(qfiNested);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfiNested))
        .thenReturn(expectedDefaultFilter2);

    // Simulate DefaultFilters creation
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));

    // Must return default filter when request filter fields is empty
    assertEquals(
        newHashSet(asList(expectedDefaultFilter1, expectedDefaultFilter2)),
        defaultFilterFactory.getDefaultFilters(SOME_INDEX, new HashSet<>()));

    // Must return default filter, when request filter fields dont match any default filter field
    assertEquals(
        newHashSet(asList(expectedDefaultFilter1, expectedDefaultFilter2)),
        defaultFilterFactory.getDefaultFilters(
            SOME_INDEX, newHashSet(asList("non", "match", "any", "defaultFilterField"))));
  }

  @Test
  public void mustNotReturnDefaultFilterForIndexWhenRequestHasFilterField() {
    QueryFragmentItem qfi = from(QueryFragmentItem.class).gimme("qfi");
    Filter filter = qfi.getFilter();

    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, newArrayList(filter.toString()));

    BoolQueryBuilder expectedBoolQuery = boolQuery();
    when(queryParser.parse(filter.toString())).thenReturn(qfi);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfi)).thenReturn(expectedBoolQuery);

    // Simulate DefaultFilters creation
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));

    // Must not return default filter when request filter match with any default filter field
    assertEquals(
        new HashSet<>(),
        defaultFilterFactory.getDefaultFilters(SOME_INDEX, singleton(filter.getField().getName())));

    // Must not return default filter when request filter match with any default filter field
    assertEquals(
        new HashSet<>(),
        defaultFilterFactory.getDefaultFilters(
            SOME_INDEX, newHashSet(asList(filter.getField().getName(), "anyOtherField"))));
  }

  @Test
  public void mustNotReturnDefaultFilterForIndexWhenRequestHasRootFilterField() {
    QueryFragmentItem qfi = from(QueryFragmentItem.class).gimme("qfiNested");
    Filter filter = qfi.getFilter();

    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, newArrayList(filter.toString()));

    BoolQueryBuilder expectedBoolQuery = boolQuery();
    when(queryParser.parse(filter.toString())).thenReturn(qfi);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfi)).thenReturn(expectedBoolQuery);

    // Simulate DefaultFilters creation
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));

    // Must not return default filter when request filter match with any default filter field
    String rootFieldName = filter.getField().getNames().stream().findFirst().get();
    assertEquals(
        new HashSet<>(),
        defaultFilterFactory.getDefaultFilters(SOME_INDEX, singleton(rootFieldName)));

    // Must not return default filter when request filter match with any default filter field
    assertEquals(
        new HashSet<>(),
        defaultFilterFactory.getDefaultFilters(
            SOME_INDEX, newHashSet(asList(rootFieldName, "anyOtherField"))));
  }

  @Test
  public void
      mustNotReturnAnyDefaultFilterForIndexWhenRequestFilterFieldMatchAllDefaultFilterFields() {
    QueryFragmentItem qfi = from(QueryFragmentItem.class).gimme("qfi");
    QueryFragmentList qfiNested =
        new QueryFragmentList(
            newArrayList(
                from(QueryFragmentItem.class).gimme("qfi"),
                from(QueryFragmentItem.class).gimme("qfiNested")));

    FILTER_DEFAULT_CLAUSES.setValue(SOME_INDEX, newArrayList(qfi.toString(), qfiNested.toString()));

    BoolQueryBuilder expectedDefaultFilter1 = boolQuery();
    when(queryParser.parse(qfi.toString())).thenReturn(qfi);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfi)).thenReturn(expectedDefaultFilter1);

    BoolQueryBuilder expectedDefaultFilter2 = boolQuery();
    when(queryParser.parse(qfiNested.toString())).thenReturn(qfiNested);
    when(filterQueryAdapter.fromQueryFragment(SOME_INDEX, qfiNested))
        .thenReturn(expectedDefaultFilter2);

    // Simulate DefaultFilters creation
    defaultFilterFactory.onApplicationEvent(new RemotePropertiesUpdatedEvent(this, SOME_INDEX));

    // Must not return any default filter since 'qfi' field is present in all default filters
    assertEquals(
        new HashSet<>(), defaultFilterFactory.getDefaultFilters(SOME_INDEX, qfi.getFieldNames()));
  }
}
