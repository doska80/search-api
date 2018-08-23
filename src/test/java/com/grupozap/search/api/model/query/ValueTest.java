package com.grupozap.search.api.model.query;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class ValueTest {

  @Test
  public void nullValue() {
    Value value = new Value(null);
    assertEquals("NULL", value.toString());
    assertEquals(0, value.size());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void nullValueWithOutOfBound() {
    new Value(null).value(0);
  }

  @Test
  public void nullValueEmptyCheck() {
    assertThat(new Value(null).contents(), empty());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void nullFirstValue() {
    new Value(null).first();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void nullLastValue() {
    new Value(null).last();
  }

  @Test
  public void nullStream() {
    assertEquals(0, new Value(null).stream().count());
  }

  @Test
  public void singleObjectValue() {
    String valueRaw = "value";
    Value value = new Value(valueRaw);
    assertEquals(valueRaw, value.first());
    assertEquals(singletonList(valueRaw), value.contents());
    assertEquals(String.format("\"%s\"", valueRaw), value.toString());
  }

  @Test
  public void multipleStringValue() {
    List<String> multiple = asList("\"value1\"", "\"value2\"", "\"value3\"");

    Value value = new Value(multiple);
    assertEquals(multiple, value.contents());
    assertEquals("[\"value1\", \"value2\", \"value3\"]", value.toString());
  }

  @Test
  public void multipleTypeValue() {
    List<Object> multiple = asList("\"value1\"", true, 42, 3.14);

    Value value = new Value(multiple);
    assertEquals(multiple, value.contents());
    assertEquals("[\"value1\", true, 42, 3.14]", value.toString());
  }

  @Test
  public void streamValue() {
    List<String> multiple = asList("\"value1\"", "\"value2\"", "\"value3\"");

    Value value = new Value(multiple);
    Stream<Object> stream = value.stream();

    assertNotNull(stream);
    List<Object> result = stream.collect(Collectors.toList());

    assertEquals(multiple, result);
  }

  @Test
  public void getRealType() {
    List<Object> multiple = asList("\"value1\"", true, 42, 3.14);

    Value value = new Value(multiple);
    assertEquals("\"value1\"", value.value(0));
    assertEquals(true, value.value(1));
    assertEquals(Integer.valueOf(42), value.value(2));
    assertEquals(Double.valueOf(3.14), value.value(3));
  }

  @Test
  public void getRealRecursiveType() {
    List<Object> multiple =
        asList(new Value("value1"), new Value(new Value(42)), new Value(asList("value2", 3.4)));

    Value value = new Value(multiple);
    assertEquals("value1", value.value(0));
    assertEquals(Integer.valueOf(42), value.value(1));
    assertEquals("value2", value.value(2));
    assertEquals(Double.valueOf(3.4), value.value(2, 1));
  }
}
