package com.grupozap.search.api.model.query;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.grupozap.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.grupozap.search.api.model.query.RelationalOperator.EQUAL;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

public class FilterTest {

  @BeforeClass
  public static void setUpClass() {
    loadAll();
  }

  @Test
  public void testToString() {
    Field field = from(Field.class).gimme("field");
    Value value = from(Value.class).gimme("value");
    var filter = new Filter(field, EQUAL, value);
    assertEquals("field EQUAL \"value\"", filter.toString());
  }
}
