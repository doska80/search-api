package com.grupozap.search.api.model.query;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.grupozap.search.api.model.query.LogicalOperator.OR;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryFragmentListTest {

  @BeforeClass
  public static void setUpClass() {
    loadAll();
  }

  @Test
  public void queryFragmentListWithSingleItem() {
    QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
    var fragmentList = singletonList(qfi);
    var qfl = new QueryFragmentList(fragmentList);

    assertEquals(fragmentList.size(), qfl.size());
    assertEquals("(field EQUAL \"value\")", qfl.toString());
    assertEquals(newHashSet("field"), qfl.getFieldNames());
    assertEquals(newHashSet("field"), qfl.getFieldNames(true));
    assertEquals(newHashSet("field"), qfl.getFieldNames(false));
    for (var i = 0; i < fragmentList.size(); i++) assertEquals(fragmentList.get(i), qfl.get(i));
  }

  @Test
  public void queryFragmentListWithMultipleItems() {
    QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
    QueryFragment qfiNested = from(QueryFragmentItem.class).gimme("qfiNested");
    List<QueryFragment> fragmentList = newArrayList(qfi, qfiNested);
    var qfl = new QueryFragmentList(fragmentList);

    Set<String> expectedFieldNamesWithoutRoot = newHashSet("field", "field1.field2.field3");
    Set<String> expectedFieldNamesWithRoot =
        newHashSet("field", "field1", "field1.field2", "field1.field2.field3");

    assertEquals(fragmentList.size(), qfl.size());
    assertEquals(
        "(field EQUAL \"value\" AND field1.field2.field3 EQUAL \"value\")", qfl.toString());
    assertEquals(expectedFieldNamesWithRoot, qfl.getFieldNames());
    assertEquals(expectedFieldNamesWithRoot, qfl.getFieldNames(true));
    assertEquals(expectedFieldNamesWithoutRoot, qfl.getFieldNames(false));
    for (var i = 0; i < fragmentList.size(); i++) assertEquals(fragmentList.get(i), qfl.get(i));
  }

  @Test
  public void queryFragmentListWithSingleRecursiveItem() {
    QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");

    var fragments = singletonList(qfi);
    List<QueryFragment> fragments1 = singletonList(new QueryFragmentList(fragments));
    List<QueryFragment> fragments2 = singletonList(new QueryFragmentList(fragments1));
    List<QueryFragment> queryFragmentList = singletonList(new QueryFragmentList(fragments2));

    var qfl = new QueryFragmentList(queryFragmentList);

    assertEquals(1, qfl.size());
    assertEquals("(field EQUAL \"value\")", qfl.toString());
    assertEquals(newHashSet("field"), qfl.getFieldNames());
    assertEquals(newHashSet("field"), qfl.getFieldNames(true));
    assertEquals(newHashSet("field"), qfl.getFieldNames(false));
  }

  @Test
  public void queryFragmentListWithNestedItem() {
    QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
    var recursion =
        new QueryFragmentList(
            newArrayList(
                from(QueryFragmentItem.class).gimme("qfiMultiple"),
                from(QueryFragmentItem.class).gimme("qfiNested")));

    var query = new QueryFragmentList(newArrayList(qfi, new QueryFragmentOperator(OR), recursion));

    Set<String> expectedFieldNamesWithoutRoot =
        newHashSet("field", "multiple", "field1.field2.field3");
    Set<String> expectedFieldNamesWithRoot =
        newHashSet("field", "multiple", "field1", "field1.field2", "field1.field2.field3");

    assertEquals(3, query.size());
    assertEquals(
        "(field EQUAL \"value\" OR (multiple EQUAL [value1, value2, value3] AND field1.field2.field3 EQUAL \"value\"))",
        query.toString());
    assertEquals(expectedFieldNamesWithRoot, query.getFieldNames());
    assertEquals(expectedFieldNamesWithRoot, query.getFieldNames(true));
    assertEquals(expectedFieldNamesWithoutRoot, query.getFieldNames(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void exceededQueryFragmentLists() {
    QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
    new QueryFragmentList(Collections.nCopies(QueryFragment.MAX_FRAGMENTS + 1, qfi));
  }
}
