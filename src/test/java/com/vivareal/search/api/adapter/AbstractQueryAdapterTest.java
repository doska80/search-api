package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.Expression;
import com.vivareal.search.api.model.Field;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.sun.tools.classfile.AccessFlags.Kind.Field;
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
        List<Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor");
        assertThat(parsedQuery.size(), is(equalTo(1)));

        Field field = parsedQuery.get(0);
        assertThat("campo", is(equalTo(field.getName())));
        assertThat(Expression.EQUAL, is(equalTo(field.getExpression())));
        assertThat("valor", is(equalTo(field.getValue())));
    }

    @Test
    public void queryWithTwoFieldsPossibleTest() {
        List<Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor campo2 EQ valor2");
        assertThat(parsedQuery.size(), is(equalTo(2)));

        Field field1 = parsedQuery.get(0);
        assertThat("campo", is(equalTo(field1.getName())));
        assertThat(Expression.EQUAL, is(equalTo(field1.getExpression())));
        assertThat("valor", is(equalTo(field1.getValue())));

        Field field2 = parsedQuery.get(1);
        assertThat("campo2", is(equalTo(field2.getName())));
        assertThat(Expression.EQUAL, is(equalTo(field2.getExpression())));
        assertThat("valor2", is(equalTo(field2.getValue())));
    }

    @Test
    public void queryWithThreeFieldsPossibleTest() {
        List<Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor campo2 GTE valor2 campo3:valor3");
        assertThat(parsedQuery.size(), is(equalTo(3)));

        Field field1 = parsedQuery.get(0);
        assertThat("campo", is(equalTo(field1.getName())));
        assertThat(Expression.EQUAL, is(equalTo(field1.getExpression())));
        assertThat("valor", is(equalTo(field1.getValue())));

        Field field2 = parsedQuery.get(1);
        assertThat("campo2", is(equalTo(field2.getName())));
        assertThat(Expression.GREATER_EQUAL, is(equalTo(field2.getExpression())));
        assertThat("valor2", is(equalTo(field2.getValue())));

        Field field3 = parsedQuery.get(2);
        assertThat("campo3", is(equalTo(field3.getName())));
        assertThat(Expression.EQUAL, is(equalTo(field3.getExpression())));
        assertThat("valor3", is(equalTo(field3.getValue())));
    }

    @Test
    public void avoidCreatingUnneededObjects() {
        List<Field> invalidQueryOne = abstractQueryAdapter.parseQuery("invalid query 1");
        List<Field> invalidQueryTwo = abstractQueryAdapter.parseQuery("another unparseable query");
        assertThat(invalidQueryOne, is(invalidQueryTwo));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void queryShouldBeImmutable() {
        List<Field> parsedQuery = abstractQueryAdapter.parseQuery("campo:valor campo2:valor2");
        parsedQuery.add(new Field("campo3", Expression.GREATER, "valor3"));
    }

}
