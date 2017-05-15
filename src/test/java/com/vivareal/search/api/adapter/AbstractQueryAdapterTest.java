package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.query.Order;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.QueryFragment;
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

    @Test
    public void avoidCreatingUnneededObjects() {
        List<QueryFragment> invalidQueryOne = abstractQueryAdapter.parseFilter("");
        List<QueryFragment> invalidQueryTwo = abstractQueryAdapter.parseFilter("");
        assertThat(invalidQueryOne, is(invalidQueryTwo));
    }

//    @Test(expected = UnsupportedOperationException.class)
//    public void queryShouldBeImmutable() {
//        List<QueryFragment> parsedQuery = abstractQueryAdapter.parseFilter("campo:valor campo2:valor2");
//        parsedQuery.add(new Field("campo3", Expression.GREATER, "valor3"));
//    }

    @Test
    public void shouldKeepSortedFieldsOrderTest() {
        List<Sort> parsedSort = abstractQueryAdapter.parseSort("field1 field2 field3 field4");
        assertThat(parsedSort.size(), is(equalTo(4)));
        assertThat(parsedSort.get(0).getName(), is(equalTo("field1")));
        assertThat(parsedSort.get(1).getName(), is(equalTo("field2")));
        assertThat(parsedSort.get(2).getName(), is(equalTo("field3")));
        assertThat(parsedSort.get(3).getName(), is(equalTo("field4")));
    }

    @Test
    public void simplestAscendingSortTest() {
        List<Sort> parsedSort = abstractQueryAdapter.parseSort("campo");
        assertThat(parsedSort.size(), is(equalTo(1)));

        Sort sort1 = parsedSort.get(0);
        assertThat(sort1.getName(), is(equalTo("campo")));
        assertThat(sort1.getOrder(), is(equalTo(Order.ASC)));
    }

    @Test
    public void ascendingSortTest() {
        List<Sort> parsedSort = abstractQueryAdapter.parseSort("campo ASC");
        assertThat(parsedSort.size(), is(equalTo(1)));

        Sort sort1 = parsedSort.get(0);
        assertThat(sort1.getName(), is(equalTo("campo")));
        assertThat(sort1.getOrder(), is(equalTo(Order.ASC)));
    }

    @Test
    public void descendingSortTest() {
        List<Sort> parsedSort = abstractQueryAdapter.parseSort("outroCampo DESC");
        assertThat(parsedSort.size(), is(equalTo(1)));

        Sort sort1 = parsedSort.get(0);
        assertThat(sort1.getName(), is(equalTo("outroCampo")));
        assertThat(sort1.getOrder(), is(equalTo(Order.DESC)));
    }

    @Test
    public void multipleSortingTest() {
        List<Sort> parsedSort = abstractQueryAdapter.parseSort("firstCampo secondCampo DESC, thirdCampo ASC, defaultSorting, anotherDefaultSorting");
        assertThat(parsedSort.size(), is(equalTo(5)));
        Sort sort1 = parsedSort.get(0);
        assertThat(sort1.getName(), is(equalTo("firstCampo")));
        assertThat(sort1.getOrder(), is(equalTo(Order.ASC)));

        Sort sort2 = parsedSort.get(1);
        assertThat(sort2.getName(), is(equalTo("secondCampo")));
        assertThat(sort2.getOrder(), is(equalTo(Order.DESC)));

        Sort sort3 = parsedSort.get(2);
        assertThat(sort3.getName(), is(equalTo("thirdCampo")));
        assertThat(sort3.getOrder(), is(equalTo(Order.ASC)));

        Sort sort4 = parsedSort.get(3);
        assertThat(sort4.getName(), is(equalTo("defaultSorting")));
        assertThat(sort4.getOrder(), is(equalTo(Order.ASC)));

        Sort sort5 = parsedSort.get(4);
        assertThat(sort5.getName(), is(equalTo("anotherDefaultSorting")));
        assertThat(sort5.getOrder(), is(equalTo(Order.ASC)));
    }

}
