package com.grupozap.search.api.service.parser.factory;

import static com.google.common.base.Objects.equal;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.FILTER_DEFAULT_CLAUSES;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import com.google.common.base.Objects;
import com.vivareal.search.api.adapter.FilterQueryAdapter;
import com.vivareal.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.QueryFragment;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DefaultFilterFactory implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFilterFactory.class);

  private final QueryParser queryParser;
  private final FilterQueryAdapter filterQueryAdapter;

  private final Map<String, Map<String, DefaultFilter>> defaultFiltersPerIndex;
  private final Map<String, Map<String, Set<String>>> defaultFiltersPerFieldForIndex;

  @Autowired
  public DefaultFilterFactory(
      @Qualifier("queryParserWithoutValidation") QueryParser queryParser,
      FilterQueryAdapter filterQueryAdapter) {
    this.queryParser = queryParser;
    this.filterQueryAdapter = filterQueryAdapter;

    this.defaultFiltersPerIndex = new ConcurrentHashMap<>();
    this.defaultFiltersPerFieldForIndex = new ConcurrentHashMap<>();
  }

  public Set<BoolQueryBuilder> getDefaultFilters(String index, Set<String> requestFilterFields) {
    Map<String, DefaultFilter> defaultFiltersForRequest =
        getDefaultFiltersForRequest(
            requestFilterFields,
            defaultFiltersPerFieldForIndex.getOrDefault(index, new HashMap<>()),
            defaultFiltersPerIndex.getOrDefault(index, new HashMap<>()));
    return defaultFiltersForRequest
        .entrySet()
        .stream()
        .map(Entry::getValue)
        .map(DefaultFilter::getQueryBuilder)
        .collect(toCollection(() -> new LinkedHashSet<>(defaultFiltersForRequest.size())));
  }

  private Map<String, DefaultFilter> getDefaultFiltersForRequest(
      Set<String> requestFilterFields,
      Map<String, Set<String>> defaultFiltersPerField,
      Map<String, DefaultFilter> defaultFiltersPerIndex) {

    Map<String, DefaultFilter> defaultFiltersForRequest =
        new LinkedHashMap<>(defaultFiltersPerIndex);

    requestFilterFields.forEach(
        field ->
            defaultFiltersPerField
                .getOrDefault(field, new HashSet<>())
                .forEach(defaultFiltersForRequest::remove));
    return defaultFiltersForRequest;
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    String index = event.getIndex();
    Set<String> rawClauses =
        (Set<String>) ofNullable(FILTER_DEFAULT_CLAUSES.getValue(index)).orElseGet(HashSet::new);
    Map<Set<String>, QueryFragment> queryFragmentsPerFields =
        rawClauses2QueryFragmentsPerFields(rawClauses);
    Set<DefaultFilter> filters = queryFragments2DefaultFilters(index, queryFragmentsPerFields);

    Map<String, DefaultFilter> defaultFiltersPerId = getDefaultFiltersPerId(filters);

    this.defaultFiltersPerIndex.put(index, defaultFiltersPerId);
    this.defaultFiltersPerFieldForIndex.put(index, getDefaultFiltersPerField(filters));

    if (!rawClauses.isEmpty()) {
      LOG.info("Refreshing default filter for index: " + index + " -- " + rawClauses);
    } else {
      LOG.debug("Index " + index + " doesnt have any default index to refresh");
    }
  }

  private Map<Set<String>, QueryFragment> rawClauses2QueryFragmentsPerFields(
      Set<String> rawClauses) {
    return rawClauses
        .stream()
        .map(queryParser::parse)
        .collect(
            toMap(
                QueryFragment::getFieldNames,
                identity(),
                (key1, key2) -> key1,
                () -> new LinkedHashMap<>(rawClauses.size())));
  }

  private Set<DefaultFilter> queryFragments2DefaultFilters(
      String index, Map<Set<String>, QueryFragment> fieldsByQueryFragment) {
    return fieldsByQueryFragment
        .entrySet()
        .stream()
        .map(entry -> asDefaultFilter(index, entry.getKey(), entry.getValue()))
        .collect(toCollection(() -> new LinkedHashSet<>(fieldsByQueryFragment.size())));
  }

  private DefaultFilter asDefaultFilter(String index, Set<String> key, QueryFragment value) {
    return new DefaultFilter(
        key, (BoolQueryBuilder) filterQueryAdapter.fromQueryFragment(index, value));
  }

  private Map<String, DefaultFilter> getDefaultFiltersPerId(Set<DefaultFilter> filters) {
    return unmodifiableMap(
        filters
            .stream()
            .collect(
                toMap(
                    DefaultFilter::getId,
                    identity(),
                    (u, v) -> u,
                    () -> new LinkedHashMap<>(filters.size()))));
  }

  private Map<String, Set<String>> getDefaultFiltersPerField(Set<DefaultFilter> filters) {
    Map<String, Set<String>> defaultFiltersPerField = new HashMap<>();
    filters.forEach(
        filter ->
            filter
                .getFields()
                .forEach(
                    field -> {
                      Set<String> defaultFilterIds =
                          defaultFiltersPerField.getOrDefault(field, new HashSet<>());
                      defaultFilterIds.add(filter.getId());
                      defaultFiltersPerField.put(field, defaultFilterIds);
                    }));
    return unmodifiableMap(defaultFiltersPerField);
  }

  private static class DefaultFilter {
    private final String id;
    private final Set<String> fields;
    private final BoolQueryBuilder queryBuilder;

    public DefaultFilter(Set<String> fields, BoolQueryBuilder queryBuilder) {
      this.fields = new TreeSet<>(fields);
      this.id = this.fields.toString();
      this.queryBuilder = queryBuilder;
    }

    public String getId() {
      return id;
    }

    public Set<String> getFields() {
      return fields;
    }

    public BoolQueryBuilder getQueryBuilder() {
      return queryBuilder;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      DefaultFilter that = (DefaultFilter) obj;
      return equal(this.id, that.id)
          && equal(this.fields, that.fields)
          && equal(this.queryBuilder, that.queryBuilder);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.id, this.fields, this.queryBuilder);
    }
  }
}
