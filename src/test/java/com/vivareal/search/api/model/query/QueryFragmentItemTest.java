package com.vivareal.search.api.model.query;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static br.com.six2six.fixturefactory.Fixture.from;
import static com.vivareal.search.api.fixtures.FixtureTemplateLoader.loadAll;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static org.junit.Assert.assertEquals;

public class QueryFragmentItemTest {

    @BeforeClass
    public static void setUpClass() {
        loadAll();
    }

    @Test
    public void queryFragmentWithLogicalOperatorToString() {
        Filter filter = from(Filter.class).gimme("filter");
        QueryFragmentItem queryFragmentItem = new QueryFragmentItem(Optional.of(AND), filter);

        assertEquals("AND field EQUAL value", queryFragmentItem.toString());
    }

    @Test
    public void queryFragmentWithoutLogicalOperatorToString() {
        Filter filter = from(Filter.class).gimme("filter");
        QueryFragmentItem queryFragmentItem = new QueryFragmentItem(Optional.empty(), filter);

        assertEquals("field EQUAL value", queryFragmentItem.toString());
    }
}
