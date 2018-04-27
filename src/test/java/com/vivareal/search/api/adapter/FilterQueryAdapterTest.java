package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.vivareal.search.api.model.parser.QueryParser;
import java.util.HashMap;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.junit.Test;

public class FilterQueryAdapterTest {

  private final FilterQueryAdapter filterQueryAdapter;
  private final QueryParser queryParser;

  public FilterQueryAdapterTest() {
    this.queryParser = queryParserFixture();
    this.filterQueryAdapter = new FilterQueryAdapter(queryParserFixture());
  }

  private BoolQueryBuilder getQueryBuilder(String field, boolean ignoreNestedQueryBuilder) {
    BoolQueryBuilder boolQueryBuilder = boolQuery();

    filterQueryAdapter.apply(
        boolQueryBuilder,
        queryParser.parse(format("%s=\"%s\"", field, "Lorem Ipsum")),
        INDEX_NAME,
        new HashMap<>(),
        ignoreNestedQueryBuilder);

    return boolQueryBuilder;
  }

  @Test
  public void shouldApplyNestedQueryBuilderWhenNecessary() {
    BoolQueryBuilder boolQueryBuilder = getQueryBuilder("nested.field", false);
    assertEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
  }

  @Test
  public void shouldIgnoreNestedQueryBuilderForNestedField() {
    BoolQueryBuilder boolQueryBuilder = getQueryBuilder("nested.field", true);
    assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
  }

  @Test
  public void shouldNotApplyNestedQueryBuilderForNonNestedField() {
    BoolQueryBuilder boolQueryBuilder = getQueryBuilder("object.field", false);
    assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
  }

  @Test
  public void shouldIgnoreNestedQueryBuilderForNotNestedField() {
    BoolQueryBuilder boolQueryBuilder = getQueryBuilder("object.field", true);
    assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
  }
}
