package com.grupozap.search.api.configuration.environment;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.util.Collections.EMPTY_SET;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;
import static org.junit.Assert.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RemotePropertiesTest {

  private static final String NON_EXISTING_INDEX = "nonExistingIndex";
  private static final String CUSTOM_INDEX = "customIndex";

  private static final Set<RemoteProperties> PROPERTIES_AS_SET =
      newLinkedHashSet(
          QS_DEFAULT_FIELDS,
          FILTER_DEFAULT_CLAUSES,
          SOURCE_INCLUDES,
          SOURCE_EXCLUDES,
          QS_TEMPLATES);

  private static final Set<RemoteProperties> INTEGER_PROPERTIES =
      newLinkedHashSet(ES_MAX_SIZE, ES_DEFAULT_SIZE, ES_FACET_SIZE, ES_STREAM_SIZE);

  private static final Set<RemoteProperties> LONG_PROPERTIES =
      newLinkedHashSet(ES_QUERY_TIMEOUT_VALUE, ES_CONTROLLER_STREAM_TIMEOUT, ES_SCROLL_KEEP_ALIVE);

  private static final Set<RemoteProperties> TIME_PROPERTIES =
      newLinkedHashSet(ES_CONTROLLER_SEARCH_TIMEOUT);

  private static final Set<RemoteProperties> TEXT_PROPERTIES =
      of(RemoteProperties.values())
          .filter(
              p ->
                  Stream.of(PROPERTIES_AS_SET, INTEGER_PROPERTIES, LONG_PROPERTIES, TIME_PROPERTIES)
                      .noneMatch(remoteProperties -> remoteProperties.contains(p)))
          .collect(toSet());

  @Before
  public void resetIndexPropertiesMap() {
    of(RemoteProperties.values()).map(RemoteProperties::getIndexProperties).forEach(Map::clear);
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
          Assert.assertEquals(property.name(), DEFAULT_INDEX, property.getValue(DEFAULT_INDEX));
          Assert.assertEquals(
              property.name(), DEFAULT_INDEX, property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void validateGetValueForNumericProperties() {
    LONG_PROPERTIES.forEach(
        property -> {
          property.setValue(CUSTOM_INDEX, MAX_VALUE);
          property.setValue(DEFAULT_INDEX, MIN_VALUE);

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
    TIME_PROPERTIES.forEach(
        property -> {
          property.setValue(CUSTOM_INDEX, 0);
          property.setValue(DEFAULT_INDEX, 10);

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
          property.setValue(CUSTOM_INDEX, customIndex);

          List<String> defaultIndex = newArrayList(DEFAULT_INDEX, DEFAULT_INDEX, DEFAULT_INDEX);
          property.setValue(DEFAULT_INDEX, defaultIndex);

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
  public void setEmptyValuesForTimeProperties() {
    TIME_PROPERTIES.forEach(
        property -> {
          try {
            property.setValue(DEFAULT_INDEX, EMPTY);
            fail(property.name() + ": Exception should be threw setting empty value");
          } catch (Exception e) {
            assertTrue(property.name(), e instanceof NumberFormatException);
          }

          assertNull(property.name(), property.getValue(DEFAULT_INDEX));
          assertNull(property.name(), property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void setEmptyValuesForLongProperties() {
    Stream.concat(TIME_PROPERTIES.stream(), LONG_PROPERTIES.stream())
        .forEach(
            property -> {
              try {
                property.setValue(DEFAULT_INDEX, EMPTY);
                fail(property.name() + ": Exception should be threw setting empty value");
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
          property.setValue(DEFAULT_INDEX, null);

          assertEquals(property.name(), EMPTY_SET, property.getValue(DEFAULT_INDEX));
          assertEquals(property.name(), EMPTY_SET, property.getValue(NON_EXISTING_INDEX));
        });
  }

  @Test
  public void checkValueIfPropertyNeverSpecified() {
    Sets.newHashSet(RemoteProperties.values()).stream()
        .filter(property -> !PROPERTIES_AS_SET.contains(property))
        .forEach(
            property -> {
              assertNull(property.name(), property.getValue(DEFAULT_INDEX));
              assertNull(property.name(), property.getValue(NON_EXISTING_INDEX));
            });
  }

  @Test
  public void checkDefaultValueForPropertiesAsSetIfNeverSpecified() {
    PROPERTIES_AS_SET.forEach(
        property -> {
          assertEquals(new HashSet<>(), property.getValue(DEFAULT_INDEX));
          assertEquals(new HashSet<>(), property.getValue(NON_EXISTING_INDEX));
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
          Assert.assertEquals(
              property.name(), DEFAULT_INDEX, property.getValue(null, DEFAULT_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(EMPTY, DEFAULT_INDEX));

          // Test over non existing index
          Assert.assertEquals(
              property.name(), DEFAULT_INDEX, property.getValue(null, NON_EXISTING_INDEX));
          assertEquals(property.name(), EMPTY, property.getValue(EMPTY, NON_EXISTING_INDEX));
        });
  }

  @Test
  public void checkIsValidRequestValueForNumericProperties() {
    Stream.concat(LONG_PROPERTIES.stream(), INTEGER_PROPERTIES.stream())
        .forEach(
            property -> {
              System.out.println("Testing remote property as numeric: " + property);
              property.setValue(CUSTOM_INDEX, MAX_VALUE);
              property.setValue(DEFAULT_INDEX, MIN_VALUE);

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
    TIME_PROPERTIES.forEach(
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
          List<String> customIndex = newArrayList(CUSTOM_INDEX, CUSTOM_INDEX, CUSTOM_INDEX);
          property.setValue(CUSTOM_INDEX, customIndex);

          List<String> defaultIndex = newArrayList(DEFAULT_INDEX, DEFAULT_INDEX, DEFAULT_INDEX);
          property.setValue(DEFAULT_INDEX, defaultIndex);

          // Test over custom index
          checkIsValidRequestForPropertyAsSet(property, customIndex, CUSTOM_INDEX);
          // Test over default index
          checkIsValidRequestForPropertyAsSet(property, defaultIndex, DEFAULT_INDEX);
          // Test over non existing index
          checkIsValidRequestForPropertyAsSet(property, defaultIndex, NON_EXISTING_INDEX);
        });
  }

  private void checkIsValidRequestForPropertyAsSet(
      RemoteProperties property, List<String> expectedIndexValue, String indexName) {
    // Use property value, since request value is null
    assertEquals(
        property.name(), new HashSet<>(expectedIndexValue), property.getValue(null, indexName));
    // Use property value, since request value is empty
    assertEquals(
        property.name(),
        new HashSet<>(expectedIndexValue),
        property.getValue(new HashSet<>(), indexName));
    // Use request value since is valid
    assertEquals(
        property.name(),
        newLinkedHashSet("requestValue"),
        property.getValue(newLinkedHashSet("requestValue"), indexName));
  }
}
