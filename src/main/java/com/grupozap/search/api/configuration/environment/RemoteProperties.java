package com.grupozap.search.api.configuration.environment;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.FieldsParser.*;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.IsRequestValidFunction.NON_EMPTY_COLLECTION;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.IsRequestValidFunction.NON_NULL_OBJECT;
import static java.lang.Long.parseLong;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;

import java.util.*;
import java.util.function.Function;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.util.CollectionUtils;

public enum RemoteProperties {
  PROFILE("spring.profiles.active"),
  QS_MM("querystring.default.mm"),
  QS_DEFAULT_FIELDS("querystring.default.fields", AS_SET),
  QS_TEMPLATES("querystring.templates", AS_SET),
  FILTER_DEFAULT_CLAUSES("filter.default.clauses", AS_SET),
  SCORE_FACTOR_FIELD("score.factor.field"),
  SCORE_FACTOR_MODIFIER("score.factor.modifier"),
  ES_HOSTNAME("es.hostname"),
  ES_REST_PORT("es.rest.port"),
  ES_CLUSTER_NAME("es.cluster.name"),
  ES_DEFAULT_SIZE("es.default.size"),
  ES_DEFAULT_SORT("es.default.sort"),
  ES_SORT_DISABLE("es.sort.disable"),
  ES_SCRIPTS("es.scripts"),
  ES_MAPPING_META_FIELDS_ID("es.mapping.meta.fields._id"),
  ES_MAX_SIZE("es.max.size"),
  ES_FACET_SIZE("es.facet.size"),
  ES_QUERY_TIMEOUT_VALUE("es.query.timeout.value", AS_LONG),
  ES_QUERY_TIMEOUT_UNIT("es.query.timeout.unit"),
  ES_CONTROLLER_SEARCH_TIMEOUT("es.controller.search.timeout", AS_TIME_VALUE_MILLIS),
  ES_CONTROLLER_STREAM_TIMEOUT("es.controller.stream.timeout", AS_LONG),
  ES_STREAM_SIZE("es.stream.size"),
  ES_SCROLL_KEEP_ALIVE("es.scroll.keep.alive", AS_LONG),
  SOURCE_INCLUDES("source.default.includes", AS_SET),
  SOURCE_EXCLUDES("source.default.excludes", AS_SET);

  public static final String DEFAULT_INDEX = "default";

  private final String property;
  private final Function<Object, ?> parser;
  private final Object defaultValueIfNeverSet;
  private final Function<Object, Boolean> isRequestValueValid;

  private final Map<String, Object> indexProperties;

  RemoteProperties(String property) {
    this(property, identity());
  }

  RemoteProperties(String property, Function<Object, ?> parser) {
    this.property = property;
    this.parser = parser;
    this.defaultValueIfNeverSet = getDefaultValueIfNeverSetForParser(parser);
    this.isRequestValueValid = getIsRequestValueValidForParser(parser);

    this.indexProperties = new HashMap<>();
  }

  String getProperty() {
    return property;
  }

  Map<String, Object> getIndexProperties() {
    return indexProperties;
  }

  public <T> T getValue(String index) {
    if (indexProperties.containsKey(index)) return (T) indexProperties.get(index);

    return (T) indexProperties.getOrDefault(DEFAULT_INDEX, defaultValueIfNeverSet);
  }

  public <T> T getValue(T requestValue, String index) {
    if (isRequestValueValid.apply(requestValue)) return requestValue;

    return getValue(index);
  }

  public void setValue(final String index, final Object value) {
    this.indexProperties.put(index, parser.apply(value));
  }

  private static Function<Object, Boolean> getIsRequestValueValidForParser(
      Function<Object, ?> parser) {
    return AS_SET.equals(parser) ? NON_EMPTY_COLLECTION : NON_NULL_OBJECT;
  }

  private static Object getDefaultValueIfNeverSetForParser(Function<Object, ?> parser) {
    return AS_SET.equals(parser) ? new HashSet<>() : null;
  }

  static class FieldsParser {

    static final Function<Object, Long> AS_LONG = integer -> parseLong(integer.toString());

    static final Function<Object, Set<String>> AS_SET =
        list ->
            ofNullable(list)
                .map(properties -> new LinkedHashSet((List) properties))
                .orElseGet(LinkedHashSet::new);

    static final Function<Object, TimeValue> AS_TIME_VALUE_MILLIS =
        rawTime -> timeValueMillis(parseLong(rawTime.toString()));
  }

  static class IsRequestValidFunction {

    static final Function<Object, Boolean> NON_NULL_OBJECT = Objects::nonNull;

    static final Function<Object, Boolean> NON_EMPTY_COLLECTION =
        collection -> !CollectionUtils.isEmpty((Collection<?>) collection);
  }
}
