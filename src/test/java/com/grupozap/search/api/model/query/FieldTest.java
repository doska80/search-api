package com.grupozap.search.api.model.query;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toCollection;
import static org.junit.Assert.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.Test;

public class FieldTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNullLinkedMapNamesFieldName() {
    new Field(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyFieldName() {
    new Field(new LinkedMap());
  }

  @Test
  public void testValidSimpleField() {
    var names = new LinkedMap();
    names.put("a", "_obj");

    var field = new Field(names);
    assertFalse(field.isNot());
    assertEquals("a", field.firstName());
    assertEquals("a", field.getName());
    assertEquals(newHashSet("a"), field.getNames());
    assertEquals("_obj", field.getType());
    assertEquals("_obj", field.getTypeFirstName());
    assertEquals("a", field.toString());
  }

  @Test
  public void testValidSimpleFieldWithNot() {
    var names = new LinkedMap();
    names.put("z", "geo_point");

    var field = new Field(true, names);
    assertTrue(field.isNot());
    assertEquals("z", field.firstName());
    assertEquals("z", field.getName());
    assertEquals(newHashSet("z"), field.getNames());
    assertEquals("geo_point", field.getType());
    assertEquals("geo_point", field.getTypeFirstName());
    assertEquals("z", field.toString());
  }

  @Test
  public void testValidNestedField() {
    var names = new LinkedMap();
    names.put("a", "_obj");
    names.put("a.b", "nested");
    names.put("a.b.c", "geo_point");

    var field = new Field(names);
    assertFalse(field.isNot());
    Set<String> expectedGetNames =
        Stream.of("a", "a.b", "a.b.c").collect(toCollection(LinkedHashSet::new));
    assertEquals("a", field.firstName());
    assertEquals(expectedGetNames, field.getNames());
    assertEquals("a.b.c", field.getName());
    assertEquals("geo_point", field.getType());
    assertEquals("_obj", field.getTypeFirstName());
    assertEquals("a.b.c", field.toString());
  }

  @Test
  public void testValidNestedFieldWithNot() {
    var names = new LinkedMap();
    names.put("x", "_obj");
    names.put("x.y", "nested");
    names.put("x.y.z", "geo_point");

    var field = new Field(true, names);
    Set<String> expectedGetNames =
        Stream.of("x", "x.y", "x.y.z").collect(toCollection(LinkedHashSet::new));
    assertTrue(field.isNot());
    assertEquals("x", field.firstName());
    assertEquals(expectedGetNames, field.getNames());
    assertEquals("x.y.z", field.getName());
    assertEquals("geo_point", field.getType());
    assertEquals("_obj", field.getTypeFirstName());
    assertEquals("x.y.z", field.toString());
  }
}
