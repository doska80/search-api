package com.vivareal.search.api.configuration.environment;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.vivareal.search.api.configuration.environment.SearchApiEnv.DEFAULT_INDEX;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

public enum RemoteProperties {

    PROFILE("spring.profiles.active"),
    QS_MM("querystring.default.mm"),
    QS_DEFAULT_FIELDS("querystring.default.fields", FieldsParser.AS_SET),
    ES_HOSTNAME("es.hostname"),
    ES_PORT("es.port"),
    ES_REST_PORT("es.rest.port"),
    ES_CLUSTER_NAME("es.cluster.name"),
    ES_DEFAULT_SIZE("es.default.size", FieldsParser.AS_INTEGER),
    ES_DEFAULT_SORT("es.default.sort"),
    ES_MAX_SIZE("es.max.size", FieldsParser.AS_INTEGER),
    ES_FACET_SIZE("es.facet.size"),
    ES_CONTROLLER_SEARCH_TIMEOUT("es.controller.search.timeout", FieldsParser.AS_LONG),
    ES_CONTROLLER_STREAM_TIMEOUT("es.controller.stream.timeout", FieldsParser.AS_INTEGER),
    ES_STREAM_SIZE("es.stream.size", FieldsParser.AS_INTEGER),
    ES_SCROLL_TIMEOUT("es.scroll.timeout", FieldsParser.AS_INTEGER),
    SOURCE_INCLUDES("source.default.includes", FieldsParser.AS_SET, IsRequestValidFunction.NON_EMPTY_COLLECTION),
    SOURCE_EXCLUDES("source.default.excludes", FieldsParser.AS_SET, IsRequestValidFunction.NON_EMPTY_COLLECTION),
    APP_PROPERTIES_INDEX("application.properties.index"),
    APP_PROPERTIES_TYPE("application.properties.type");

    private String property;
    private Map<String, Object> indexProperties;
    private Function<String, ?> parser;
    private Function<Object, Boolean> isRequestValueValid;

    RemoteProperties(String property) {
        this(property, FieldsParser.AS_STRING);
    }

    RemoteProperties(String property, Function<String, ?> parser) {
        this(property, parser, IsRequestValidFunction.NON_NULL_OBJECT);
    }

    RemoteProperties(String property, Function<String, ?> parser, Function<Object, Boolean> isRequestValueValid) {
        this.property = property;
        this.parser = parser;
        this.isRequestValueValid = isRequestValueValid;

        this.indexProperties = new HashMap<>();
    }


    public String getProperty() {
        return property;
    }

    public <T> T getValue(String index) {
        if (indexProperties.containsKey(index))
            return (T) indexProperties.get(index);

        return (T) indexProperties.get(DEFAULT_INDEX);
    }

    public <T> T getValue(T requestValue, String index) {
        if(isRequestValueValid.apply(requestValue))
            return requestValue;

        return getValue(index);
    }

    public void setValue(final String index, final String value) {
        this.indexProperties.put(index, parser.apply(value));
    }

    private static class FieldsParser {

        private static Function<String, String> AS_STRING = Function.identity();

        private static Function<String, Set<String>> AS_SET = property -> ofNullable(property)
                .filter(StringUtils::isNotBlank)
                .map(value -> value.split(","))
                .map(stringArray -> Stream.of(stringArray).collect(toSet()))
                .orElse(new HashSet<>());

        private static Function<String, Integer> AS_INTEGER = property -> parseInt(property);

        private static Function<String, Long> AS_LONG = property -> parseLong(property);
    }

    private static class IsRequestValidFunction {

        private static Function<Object, Boolean> NON_NULL_OBJECT = Objects::nonNull;

        private static Function<Object, Boolean> NON_EMPTY_COLLECTION = collection -> !CollectionUtils.isEmpty((Collection<?>) collection);

    }
}
