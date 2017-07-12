package com.vivareal.search.api.model.query;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class QueryFragmentListTest {

    @BeforeClass
    public static void setUpClass() {
        loadAll();
    }

    @Test
    public void queryFragmentListWithSingleItem() {
        QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
        List<QueryFragment> fragmentList = singletonList(qfi);
        QueryFragmentList qfl = new QueryFragmentList(fragmentList);

        assertEquals(fragmentList.size(), qfl.size());
        assertEquals("(field EQUAL \"value\")", qfl.toString());
        for (int i = 0; i < fragmentList.size(); i++)
            assertEquals(fragmentList.get(i), qfl.get(i));
    }

    @Test
    public void queryFragmentListWithSingleNestedItem() {
        QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");

        List<QueryFragment> fragments = singletonList(qfi);
        List<QueryFragment> fragments1 = singletonList(new QueryFragmentList(fragments));
        List<QueryFragment> fragments2 = singletonList(new QueryFragmentList(fragments1));
        List<QueryFragment> queryFragmentList = singletonList(new QueryFragmentList(fragments2));

        QueryFragmentList qfl = new QueryFragmentList(queryFragmentList);

        assertEquals(1, qfl.size());
        assertEquals("(field EQUAL \"value\")", qfl.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceededQueryFragmentLists() {
        QueryFragment qfi = from(QueryFragmentItem.class).gimme("qfi");
        new QueryFragmentList(Collections.nCopies(QueryFragment.MAX_FRAGMENTS + 1, qfi));
    }
}
