package com.vivareal.search.api.configuration.environment;

import static com.google.common.collect.Lists.newArrayList;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.lang.String.valueOf;
import static java.util.Collections.EMPTY_SET;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;
import static org.junit.Assert.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class RemotePropertiesTest {

  private static final String NON_EXISTING_INDEX = "nonExistingIndex";
  private static final String CUSTOM_INDEX = "customIndex";

  private static final Set<RemoteProperties> PROPERTIES_AS_SET =
      newLinkedHashSet(QS_DEFAULT_FIELDS, FILTER_DEFAULT_CLAUSES, SOURCE_INCLUDES, SOURCE_EXCLUDES);
  private static final Set<RemoteProperties> NUMERIC_PROPERTIES =
      newLinkedHashSet(
          ES_DEFAULT_SIZE,
          ES_MAX_SIZE,
          ES_FACET_SIZE,
          ES_QUERY_TIMEOUT_VALUE,
          ES_STREAM_SIZE,
          ES_SCROLL_TIMEOUT,
          ES_CONTROLLER_STREAM_TIMEOUT);
  private static final Set<RemoteProperties> TIME_AS_SET =
      newLinkedHashSet(ES_CONTROLLER_SEARCH_TIMEOUT);
  private static final Set<RemoteProperties> TEXT_PROPERTIES =
      of(values())
          .filter(
              p ->
                  !PROPERTIES_AS_SET.contains(p)
                      && !NUMERIC_PROPERTIES.contains(p)
                      && !TIME_AS_SET.contains(p))
          .collect(toSet());

  @Before
  public void resetIndexPropertiesMap() {
    of(values()).map(RemoteProperties::getIndexProperties).forEach(Map::clear);
  }

  // ****************************
  // Get value tests
  // ****************************

  @Test
  public void validateGetValueForTextProperties() {
    TEXT_PROPERTIES.forEach(
        property -> {
          property.setValue(CUSTOM_INDEX, CUSTOM_INDEX);
          property.setValue(DEFAULT_INDEX, DEFAULT_INDEX);

          assertEquals(property.name(), CUSTOM_INDEX, property.getValue(CUSTOM_INDEX));
          assertEquals(property.name(), DEFAULT_INDEX, property.getValue(DEFAULT_INDEX));
          assertEquals(property.name(), DEFAULT_INDEX, property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void validateGetValueForNumericProperties() {
    NUMERIC_PROPERTIES.forEach(
        property -> {
          String minValue = valueOf(MIN_VALUE);
          String maxValue = valueOf(MAX_VALUE);

          property.setValue(CUSTOM_INDEX, maxValue);
          property.setValue(DEFAULT_INDEX, minValue);

          assertEquals(
              property.name(),
              numberAsInteger(MAX_VALUE),
              numberAsInteger(property.getValue(CUSTOM_INDEX)));
          assertEquals(
              property.name(),
              numberAsInteger(MIN_VALUE),
              numberAsInteger(property.getValue(DEFAULT_INDEX)));
          assertEquals(
              property.name(),
              numberAsInteger(MIN_VALUE),
              numberAsInteger(property.getValue(NON_EXISTING_INDEX)));
        });
  }

  @Test
  public void validateGetValueForTimeValueProperties() {
    TIME_AS_SET.forEach(
        property -> {
          property.setValue(CUSTOM_INDEX, "0");
          property.setValue(DEFAULT_INDEX, "10");

          assertEquals(property.name(), timeValueMillis(0), property.getValue(CUSTOM_INDEX));
          assertEquals(property.name(), timeValueMillis(10), property.getValue(DEFAULT_INDEX));
          assertEquals(property.name(), timeValueMillis(10), property.getValue(NON_EXISTING_INDEX));
        });
  }

  private Integer numberAsInteger(Number number) {
    return number.intValue();
  }

  @Test
  public void validateGetValueForPropertiesAsSet() {
    PROPERTIES_AS_SET.forEach(
        property -> {
          List<String> customIndex = newArrayList(CUSTOM_INDEX, CUSTOM_INDEX, CUSTOM_INDEX);
          property.setValue(CUSTOM_INDEX, customIndex.stream().collect(joining(",")));

          List<String> defaultIndex = newArrayList(DEFAULT_INDEX, DEFAULT_INDEX, DEFAULT_INDEX);
          property.setValue(DEFAULT_INDEX, defaultIndex.stream().collect(joining(",")));

          assertEquals(
              property.name(), new HashSet<>(customIndex), property.getValue(CUSTOM_INDEX));
          assertEquals(
              property.name(), new HashSet<>(defaultIndex), property.getValue(DEFAULT_INDEX));
          assertEquals(
              property.name(), new HashSet<>(defaultIndex), property.getValue(NON_EXISTING_INDEX));
        });
  }

  // ****************************
  // Empty properties test
  // ****************************

  @Test
  public void setEmptyValuesForTextProperties() {
    TEXT_PROPERTIES.forEach(
        property -> {
          property.setValue(DEFAULT_INDEX, EMPTY);

          assertEquals(property.name(), EMPTY, property.getValue(DEFAULT_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void setEmptyValuesForNumericProperties() {
    Stream.concat(TIME_AS_SET.stream(), NUMERIC_PROPERTIES.stream())
        .forEach(
            property -> {
              try {
                property.setValue(DEFAULT_INDEX, EMPTY);
                assertFalse(
                    property.name() + ": Exception should be threw setting empty value", true);
              } catch (Exception e) {
                assertTrue(property.name(), e instanceof NumberFormatException);
              }

              assertNull(property.name(), property.getValue(DEFAULT_INDEX));
              assertNull(property.name(), property.getValue(NON_EXISTING_INDEX));
            });
  }

  @Test
  public void setEmptyValuesForPropertiesAsSet() {
    PROPERTIES_AS_SET.forEach(
        property -> {
          property.setValue(DEFAULT_INDEX, EMPTY);

          assertEquals(property.name(), EMPTY_SET, property.getValue(DEFAULT_INDEX));
          assertEquals(property.name(), EMPTY_SET, property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void checkValueIfPropertyNeverSet() {
    of(values())
        .forEach(
            property -> {
              assertNull(property.name(), property.getValue(DEFAULT_INDEX));
              assertNull(property.name(), property.getValue(NON_EXISTING_INDEX));
            });
  }

  // *****************************************
  // Get default value if request is empty
  // *****************************************

  @Test
  public void checkIsValidRequestValueForTextProperties() {
    TEXT_PROPERTIES.forEach(
        property -> {
          System.out.println("Testing remote property as text: " + property);
          property.setValue(CUSTOM_INDEX, CUSTOM_INDEX);
          property.setValue(DEFAULT_INDEX, DEFAULT_INDEX);

          // Test over custom index
          assertEquals(property.name(), CUSTOM_INDEX, property.getValue(null, CUSTOM_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(EMPTY, CUSTOM_INDEX));

          // Test over default index
          assertEquals(property.name(), DEFAULT_INDEX, property.getValue(null, DEFAULT_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(EMPTY, DEFAULT_INDEX));

          // Test over non existing index
          assertEquals(property.name(), DEFAULT_INDEX, property.getValue(null, NON_EXISTING_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(EMPTY, NON_EXISTING_INDEX));
        });
  }

  @Test
  public void checkIsValidRequestValueForNumericProperties() {
    NUMERIC_PROPERTIES.forEach(
        property -> {
          System.out.println("Testing remote property as numeric: " + property);
          String minValue = valueOf(MIN_VALUE);
          String maxValue = valueOf(MAX_VALUE);

          property.setValue(CUSTOM_INDEX, maxValue);
          property.setValue(DEFAULT_INDEX, minValue);

          // Test over custom index
          assertEquals(
              property.name(),
              numberAsInteger(MAX_VALUE),
              numberAsInteger(property.getValue(null, CUSTOM_INDEX)));
          assertEquals(
              property.name(),
              numberAsInteger(0),
              numberAsInteger(property.getValue(0, CUSTOM_INDEX)));

          // Test over default index
          assertEquals(
              property.name(),
              numberAsInteger(MIN_VALUE),
              numberAsInteger(property.getValue(null, DEFAULT_INDEX)));
          assertEquals(
              property.name(),
              numberAsInteger(0),
              numberAsInteger(property.getValue(0, DEFAULT_INDEX)));

          // Test over non existing index
          assertEquals(
              property.name(),
              numberAsInteger(MIN_VALUE),
              numberAsInteger(property.getValue(null, NON_EXISTING_INDEX)));
          assertEquals(
              property.name(),
              numberAsInteger(0),
              numberAsInteger(property.getValue(0, NON_EXISTING_INDEX)));
        });
  }

  @Test
  public void checkIsValidRequestValueForTimeValueProperties() {
    TIME_AS_SET.forEach(
        property -> {
          System.out.println("Testing remote property as TimeValue: " + property);

          property.setValue(CUSTOM_INDEX, "0");
          property.setValue(DEFAULT_INDEX, "10");

          // Test over custom index
          assertEquals(property.name(), timeValueMillis(0), property.getValue(null, CUSTOM_INDEX));
          assertEquals(
              property.name(),
              timeValueMillis(11),
              property.getValue(timeValueMillis(11), CUSTOM_INDEX));

          // Test over default index
          assertEquals(
              property.name(), timeValueMillis(10), property.getValue(null, DEFAULT_INDEX));
          assertEquals(
              property.name(),
              timeValueMillis(12),
              property.getValue(timeValueMillis(12), DEFAULT_INDEX));

          // Test over non existing index
          assertEquals(
              property.name(), timeValueMillis(10), property.getValue(null, NON_EXISTING_INDEX));
          assertEquals(
              property.name(),
              timeValueMillis(13),
              property.getValue(timeValueMillis(13), NON_EXISTING_INDEX));
        });
  }

  @Test
  public void checkIsValidRequestValueForPropertiesAsSet() {
    PROPERTIES_AS_SET.forEach(
        property -> {
          System.out.println("Testing remote property as set: " + property);
          Set<String> customIndex = newLinkedHashSet(CUSTOM_INDEX, CUSTOM_INDEX, CUSTOM_INDEX);
          property.setValue(CUSTOM_INDEX, customIndex.stream().collect(joining(",")));

          Set<String> defaultIndex = newLinkedHashSet(DEFAULT_INDEX, DEFAULT_INDEX, DEFAULT_INDEX);
          property.setValue(DEFAULT_INDEX, defaultIndex.stream().collect(joining(",")));

          // Test over custom index
          checkIsValidRequestForProperty(property, customIndex, CUSTOM_INDEX);
          // Test over default index
          checkIsValidRequestForProperty(property, defaultIndex, DEFAULT_INDEX);
          // Test over non existing index
          checkIsValidRequestForProperty(property, defaultIndex, NON_EXISTING_INDEX);
        });
  }

  private void checkIsValidRequestForProperty(
      RemoteProperties property, Set<String> expectedIndexValue, String indexName) {
    assertEquals(property.name(), expectedIndexValue, property.getValue(null, indexName));
    assertEquals(
        property.name(), expectedIndexValue, property.getValue(new HashSet<>(), indexName));
    assertEquals(
        property.name(),
        newLinkedHashSet("requestValue"),
        property.getValue(newLinkedHashSet("requestValue"), indexName));
  }
}
