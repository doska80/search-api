package com.grupozap.search.api.itest.configuration.data;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TestData {

  private static final int GEOPOINT_MAX_VALUE = 90;
  private static final int FIELD_MAX_VALUE = 100;

  private static List<Map<String, Object>> getNestedArray(int id) {
    Map<String, Object> a =
        unmodifiableMap(
            Stream.of(
                    new SimpleEntry<>("string", "a"),
                    new SimpleEntry<>("id", id),
                    new SimpleEntry<>("number", id > 15 ? 100 * id : 100 / id))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    Map<String, Object> b =
        unmodifiableMap(
            Stream.of(
                    new SimpleEntry<>("string", "b"),
                    new SimpleEntry<>("id", id),
                    new SimpleEntry<>("number", id < 15 ? 1000 * id : 1000 / id))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    ArrayList<Map<String, Object>> nested = newArrayList();

    if ((id % 3) == 0) nested.add(a);
    else if ((id % 4) == 0) nested.add(b);
    else {
      nested.add(a);
      nested.add(b);
    }

    return nested;
  }

  public static Map<String, Object> createTestData(int id, int facetValue) {
    var isEven = isEven(id);

    Map<String, Object> kv =
        unmodifiableMap(
            Stream.of(
                    new SimpleEntry<>("field", "common"),
                    new SimpleEntry<>("array_string", idsBetween(1, id)))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    var nestedObject =
        unmodifiableMap(
            Stream.of(
                    new SimpleEntry<>("object", kv),
                    new SimpleEntry<>("id", id),
                    new SimpleEntry<>("number", numberForId(id)),
                    new SimpleEntry<>("float", floatForId(id)),
                    new SimpleEntry<>("string", normalTextForId(id)),
                    new SimpleEntry<>("string_text", normalTextForId(id)),
                    new SimpleEntry<>("special_string", specialTextForId(id)),
                    new SimpleEntry<>("boolean", !isEven),
                    new SimpleEntry<>(evenField(id), true))
                .collect(toMap(SimpleEntry::getKey, e -> (Object) e.getValue())));

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", valueOf(id));
    data.put("numeric", id);
    data.put("decreasing_number", FIELD_MAX_VALUE - id);
    data.put("priority_x", id);
    data.put("isEven", isEven(id));
    data.put("array_integer", rangeClosed(1, id).boxed().collect(toList()));
    data.put("nested", nestedObject);
    data.put("object", nestedObject);
    data.put("nested_array", getNestedArray(id));

    var maxFieldId = id % FIELD_MAX_VALUE;
    data.put("field" + maxFieldId, "value" + maxFieldId);
    data.put("geo", geoField(id));
    data.put("field_after_alias", id);
    data.put("field_geo_after_alias", geoField(id));
    data.putAll(facetsData(id, facetValue));
    return data;
  }

  private static Map<String, Object> facetsData(int id, int facetValue) {
    return unmodifiableMap(
        Stream.of(
                new SimpleEntry<>("facetString", charForId(facetValue)),
                new SimpleEntry<>("facetInteger", facetValue),
                new SimpleEntry<>("facetBoolean", facetValue == 1))
            .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
  }

  private static Map<String, Float> geoField(int id) {
    return unmodifiableMap(
        Stream.of(new SimpleEntry<>("lat", latitude(id)), new SimpleEntry<>("lon", longitude(id)))
            .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
  }

  public static float floatForId(int id) {
    return id * 3.5f;
  }

  public static boolean isEven(int id) {
    return id % 2 == 0;
  }

  public static float longitude(int id) {
    return id % GEOPOINT_MAX_VALUE * 1f;
  }

  public static float latitude(int id) {
    return longitude(id) * -1f;
  }

  public static boolean isOdd(int id) {
    return !isEven(id);
  }

  public static List<String> idsBetween(int from, int to) {
    return rangeClosed(from, to).boxed().map(String::valueOf).collect(toList());
  }

  public static char charForId(int id) {
    return (char) (id + 'A' - 1);
  }

  public static String normalTextForId(int id) {
    return format("string with char %s", charForId(id));
  }

  public static String specialTextForId(int id) {
    return format(
        "string with special chars * and + and %n and ? and %% and 5%% and _ and with_underscore of %s to search by like",
        (char) (id + 'a' - 1));
  }

  public static String evenField(int id) {
    return isEven(id) ? "even" : "odd";
  }

  public static int numberForId(int id) {
    return id * 2;
  }
}
