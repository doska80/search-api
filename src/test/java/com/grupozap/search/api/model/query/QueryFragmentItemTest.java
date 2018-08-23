package com.grupozap.search.api.model.query;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryFragmentItemTest {

  @BeforeClass
  public static void setUpClass() {
    loadAll();
  }

  @Test
  public void queryFragmentWithLogicalOperatorToString() {
    Filter filter = from(Filter.class).gimme("filter");
    QueryFragmentItem queryFragmentItem = new QueryFragmentItem(Optional.of(AND), filter);

    assertEquals("AND field EQUAL \"value\"", queryFragmentItem.toString());
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames());
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames(true));
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames(false));
  }

  @Test
  public void fieldNamesForQueryFragmentWithFieldWithDots() {
    Filter filter = from(Filter.class).gimme("nested");
    QueryFragmentItem queryFragmentItem = new QueryFragmentItem(Optional.empty(), filter);

    Set<String> expectedFieldNamesWithRoot =
        newLinkedHashSet(asList("field1", "field1.field2", "field1.field2.field3"));
    assertEquals("field1.field2.field3 EQUAL \"value\"", queryFragmentItem.toString());
    assertEquals(expectedFieldNamesWithRoot, queryFragmentItem.getFieldNames());
    assertEquals(expectedFieldNamesWithRoot, queryFragmentItem.getFieldNames(true));
    assertEquals(newHashSet("field1.field2.field3"), queryFragmentItem.getFieldNames(false));
  }

  @Test
  public void queryFragmentWithoutLogicalOperatorToString() {
    Filter filter = from(Filter.class).gimme("filter");
    QueryFragmentItem queryFragmentItem = new QueryFragmentItem(Optional.empty(), filter);

    assertEquals("field EQUAL \"value\"", queryFragmentItem.toString());
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames());
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames(true));
    assertEquals(newHashSet("field"), queryFragmentItem.getFieldNames(true));
  }
}
