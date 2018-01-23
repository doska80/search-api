package com.vivareal.search.api.model.query;

import static org.junit.Assert.assertEquals;

import org.apache.commons.collections.map.LinkedMap;
import org.junit.Test;

public class FieldTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNullFieldName() {
    new Field(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyFieldName() {
    new Field(new LinkedMap());
  }

  @Test
  public void testValidSimpleField() {
    LinkedMap names = new LinkedMap();
    names.put("a", "_obj");

    Field field = new Field(names);
    assertEquals("a", field.firstName());
    assertEquals("a", field.getName());
    assertEquals("_obj", field.getType());
    assertEquals("_obj", field.getTypeFirstName());
    assertEquals("a", field.toString());
  }

  @Test
  public void testValidNestedField() {
    LinkedMap names = new LinkedMap();
    names.put("a", "_obj");
    names.put("a.b", "nested");
    names.put("a.b.c", "geo_point");

    Field field = new Field(names);
    assertEquals("a", field.firstName());
    assertEquals("a.b.c", field.getName());
    assertEquals("geo_point", field.getType());
    assertEquals("_obj", field.getTypeFirstName());
    assertEquals("a.b.c", field.toString());
  }
}
