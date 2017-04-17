package com.vivareal.search.api.adapter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringRunner.class)
public class AbstractQueryAdapterTest {

    private static TestQueryAdapter abstractQueryAdapter;

    @BeforeClass
    public static void setup() {
        abstractQueryAdapter = new TestQueryAdapter();
    }

    @Test
    public void simplestQueryPossibleTest() {
        List<AbstractQueryAdapter.Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor");
        assertThat(parsedQuery.size(), is(equalTo(1)));

        AbstractQueryAdapter.Field field = parsedQuery.get(0);
        assertThat("campo", is(equalTo(field.getName())));
        assertThat(AbstractQueryAdapter.Expression.EQUAL, is(equalTo(field.getExpression())));
        assertThat("valor", is(equalTo(field.getValue())));
    }

    @Test
    public void queryWithTwoFieldsPossibleTest() {
        List<AbstractQueryAdapter.Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor campo2:valor2");
        assertThat(parsedQuery.size(), is(equalTo(2)));

        AbstractQueryAdapter.Field field1 = parsedQuery.get(0);
        assertThat("campo", is(equalTo(field1.getName())));
        assertThat(AbstractQueryAdapter.Expression.EQUAL, is(equalTo(field1.getExpression())));
        assertThat("valor", is(equalTo(field1.getValue())));

        AbstractQueryAdapter.Field field2 = parsedQuery.get(1);
        assertThat("campo2", is(equalTo(field2.getName())));
        assertThat(AbstractQueryAdapter.Expression.EQUAL, is(equalTo(field2.getExpression())));
        assertThat("valor2", is(equalTo(field2.getValue())));
    }

    @Test
    public void avoidCreatingUnneededObjects() {
        List<AbstractQueryAdapter.Field> invalidQueryOne = abstractQueryAdapter.parseQuery("invalid query 1");
        List<AbstractQueryAdapter.Field> invalidQueryTwo = abstractQueryAdapter.parseQuery("another unparseable query");
        assertThat(invalidQueryOne, is(invalidQueryTwo));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void queryShouldBeImmutable() {
        List<AbstractQueryAdapter.Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor campo2:valor2");
        parsedQuery.add(abstractQueryAdapter.new Field("campo3", AbstractQueryAdapter.Expression.GREATER, "valor3"));
    }

}
