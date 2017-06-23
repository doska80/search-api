package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.query.Sort;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static com.vivareal.search.api.model.query.OrderOperator.ASC;
import static com.vivareal.search.api.model.query.OrderOperator.DESC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringRunner.class)
public class AbstractQueryAdapterTest {

    private static TestQueryAdapter abstractQueryAdapter;

    @BeforeClass
    public static void setup() {
        abstractQueryAdapter = new TestQueryAdapter();
    }

//    @Test
//    public void simplestQueryPossibleTest() {
//        List<QueryFragment> parsedQuery = abstractQueryAdapter.parseFilter("campo:valor");
//        assertThat(parsedQuery.size(), is(equalTo(1)));
//
//        Field field = parsedQuery.get(0);
//        assertThat("campo", is(equalTo(field.getName())));
//        assertThat(Expression.EQUAL, is(equalTo(field.getExpression())));
//        assertThat("valor", is(equalTo(field.getValue())));
//    }
//
//    @Test
//    public void queryWithTwoFieldsPossibleTest() {
//        List<QueryFragment> parsedQuery = abstractQueryAdapter.parseFilter("campo:valor campo2 EQ valor2");
//        assertThat(parsedQuery.size(), is(equalTo(2)));
//
//        QueryFragment field1 = parsedQuery.get(0);
//        assertThat("campo", is(equalTo(field1.getName())));
//        assertThat(Expression.EQUAL, is(equalTo(field1.getExpression())));
//        assertThat("valor", is(equalTo(field1.getValue())));
//
//        QueryFragment field2 = parsedQuery.get(1);
//        assertThat("campo2", is(equalTo(field2.getName())));
//        assertThat(Expression.EQUAL, is(equalTo(field2.getExpression())));
//        assertThat("valor2", is(equalTo(field2.getValue())));
//    }
//
//    @Test
//    public void queryWithThreeFieldsPossibleTest() {
//        List<QueryFragment> parsedQuery = abstractQueryAdapter.parseFilter("campo:valor campo2 GT valor2 campo3:valor3");
//        assertThat(parsedQuery.size(), is(equalTo(3)));
//
//        QueryFragment field1 = parsedQuery.get(0);
//        assertThat("campo", is(equalTo(field1.getName())));
//        assertThat(Expression.EQUAL, is(equalTo(field1.getExpression())));
//        assertThat("valor", is(equalTo(field1.getValue())));
//
//        QueryFragment field2 = parsedQuery.get(1);
//        assertThat("campo2", is(equalTo(field2.getName())));
//        assertThat(Expression.GREATER, is(equalTo(field2.getExpression())));
//        assertThat("valor2", is(equalTo(field2.getValue())));
//
//        QueryFragment field3 = parsedQuery.get(2);
//        assertThat("campo3", is(equalTo(field3.getName())));
//        assertThat(Expression.EQUAL, is(equalTo(field3.getExpression())));
//        assertThat("valor3", is(equalTo(field3.getValue())));
//    }
//
//    @Test
//    public void avoidCreatingUnneededObjects() {
//        List<QueryFragment> invalidQueryOne = abstractQueryAdapter.parseFilter("");
//        List<QueryFragment> invalidQueryTwo = abstractQueryAdapter.parseFilter("");
//        assertThat(invalidQueryOne, is(invalidQueryTwo));
//    }
//
//    @Test(expected = UnsupportedOperationException.class)
//    public void queryShouldBeImmutable() {
//        List<QueryFragment> parsedQuery = abstractQueryAdapter.parseFilter("campo:valor campo2:valor2");
//        parsedQuery.add(new Field("campo3", Expression.GREATER, "valor3"));
//    }

    @Test
    public void shouldKeepSortedFieldsOrderTest() {
        Sort sort = abstractQueryAdapter.parseSort("field1, field2 DESC, field3 ASC, field4 DESC");
        assertThat(sort, hasSize(4));
        assertThat(sort.get(0).getField().getName(), is(equalTo("field1")));
        assertThat(sort.get(0).getOrderOperator(), is(equalTo(ASC)));

        assertThat(sort.get(1).getField().getName(), is(equalTo("field2")));
        assertThat(sort.get(1).getOrderOperator(), is(equalTo(DESC)));

        assertThat(sort.get(2).getField().getName(), is(equalTo("field3")));
        assertThat(sort.get(2).getOrderOperator(), is(equalTo(ASC)));

        assertThat(sort.get(3).getField().getName(), is(equalTo("field4")));
        assertThat(sort.get(3).getOrderOperator(), is(equalTo(DESC)));
    }
}
