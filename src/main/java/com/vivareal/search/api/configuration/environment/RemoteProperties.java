package com.vivareal.search.api.configuration.environment;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.FieldsParser.*;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.IsRequestValidFunction.NON_EMPTY_COLLECTION;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.IsRequestValidFunction.NON_NULL_OBJECT;
import static java.lang.Long.parseLong;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.util.CollectionUtils;

public enum RemoteProperties {
  PROFILE("spring.profiles.active"),
  QS_MM("querystring.default.mm"),
  QS_DEFAULT_FIELDS("querystring.default.fields", AS_SET, NON_EMPTY_COLLECTION),
  FILTER_DEFAULT_CLAUSES("filter.default.clauses", AS_SET, NON_EMPTY_COLLECTION),
  SCORE_FACTOR_FIELD("score.factor.field"),
  SCORE_FACTOR_MODIFIER("score.factor.modifier"),
  ES_HOSTNAME("es.hostname"),
  ES_PORT("es.port"),
  ES_REST_PORT("es.rest.port"),
  ES_CLUSTER_NAME("es.cluster.name"),
  ES_DEFAULT_SIZE("es.default.size", AS_INTEGER),
  ES_DEFAULT_SORT("es.default.sort"),
  ES_MAPPING_META_FIELDS_ID("es.mapping.meta.fields._id"),
  ES_MAX_SIZE("es.max.size", AS_INTEGER),
  ES_FACET_SIZE("es.facet.size", AS_INTEGER),
  ES_QUERY_TIMEOUT_VALUE("es.query.timeout.value", AS_LONG),
  ES_QUERY_TIMEOUT_UNIT("es.query.timeout.unit"),
  ES_CONTROLLER_SEARCH_TIMEOUT("es.controller.search.timeout", AS_TIME_VALUE_MILLIS),
  ES_CONTROLLER_STREAM_TIMEOUT("es.controller.stream.timeout", AS_TIME_VALUE_MILLIS),
  ES_STREAM_SIZE("es.stream.size", AS_INTEGER),
  ES_SCROLL_TIMEOUT("es.scroll.timeout", AS_INTEGER),
  SOURCE_INCLUDES("source.default.includes", AS_SET, NON_EMPTY_COLLECTION),
  SOURCE_EXCLUDES("source.default.excludes", AS_SET, NON_EMPTY_COLLECTION),
  APP_PROPERTIES_INDEX("application.properties.index"),
  APP_PROPERTIES_TYPE("application.properties.type");

  public static final String DEFAULT_INDEX = "default";

  private final String property;
  private final Function<String, ?> parser;
  private final Function<Object, Boolean> isRequestValueValid;

  private final Map<String, Object> indexProperties;

  RemoteProperties(String property) {
    this(property, AS_STRING);
  }

  RemoteProperties(String property, Function<String, ?> parser) {
    this(property, parser, NON_NULL_OBJECT);
  }

  RemoteProperties(
      String property, Function<String, ?> parser, Function<Object, Boolean> isRequestValueValid) {
    this.property = property;
    this.parser = parser;
    this.isRequestValueValid = isRequestValueValid;

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

    return (T) indexProperties.get(DEFAULT_INDEX);
  }

  public <T> T getValue(T requestValue, String index) {
    if (isRequestValueValid.apply(requestValue)) return requestValue;

    return getValue(index);
  }

  public void setValue(final String index, final String value) {
    this.indexProperties.put(index, parser.apply(value));
  }

  static class FieldsParser {

    static final Function<String, String> AS_STRING = Function.identity();

    static final Function<String, Set<String>> AS_SET =
        property ->
            ofNullable(property)
                .filter(StringUtils::isNotBlank)
                .map(value -> value.split(","))
                .map(stringArray -> Stream.of(stringArray).collect(toSet()))
                .orElse(emptySet());

    static final Function<String, Integer> AS_INTEGER = Integer::parseInt;

    static final Function<String, Long> AS_LONG = Long::parseLong;

    static final Function<String, TimeValue> AS_TIME_VALUE_MILLIS =
        s -> timeValueMillis(parseLong(s));
  }

  static class IsRequestValidFunction {

    static final Function<Object, Boolean> NON_NULL_OBJECT = Objects::nonNull;

    static final Function<Object, Boolean> NON_EMPTY_COLLECTION =
        collection -> !CollectionUtils.isEmpty((Collection<?>) collection);
  }
}
