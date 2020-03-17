package com.grupozap.search.api.fixtures.model.parser;

import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.grupozap.search.api.listener.ESSortListener;
import com.grupozap.search.api.model.parser.FacetParser;
import com.grupozap.search.api.model.parser.FieldParser;
import com.grupozap.search.api.model.parser.FilterParser;
import com.grupozap.search.api.model.parser.NotParser;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.parser.ValueParser;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.service.parser.IndexSettings;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import com.grupozap.search.api.service.parser.factory.FieldFactory;
import com.grupozap.search.api.service.parser.factory.SearchAlias;
import com.grupozap.search.api.service.parser.factory.SearchAlias.SearchAliasProp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.mockito.stubbing.Answer;

public class ParserTemplateLoader {

  public static QueryParser queryParserFixture() {
    var operatorParser = new OperatorParser();
    var fieldParser = fieldParserFixture();
    var filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    return new QueryParser(operatorParser, filterParser, new NotParser());
  }

  public static QueryParser queryParserWithOutValidationFixture() {
    var operatorParser = new OperatorParser();
    var fieldParser = fieldParserWithoutValidationFixture();
    var filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    return new QueryParser(operatorParser, filterParser, new NotParser());
  }

  public static SortParser sortParserFixture() {
    return new SortParser(
        fieldParserWithoutValidationFixture(),
        new OperatorParser(),
        new ValueParser(),
        queryParserFixture());
  }

  public static FacetParser facetParserFixture() {
    return new FacetParser(fieldParserFixture(), sortParserFixture());
  }

  public static FilterParser filterParserFixture() {
    return new FilterParser(fieldParserFixture(), new OperatorParser(), new ValueParser());
  }

  public static FieldCache fieldCacheFixture() {
    var indexSettings = mock(IndexSettings.class);
    when(indexSettings.getIndex()).thenReturn(INDEX_NAME);

    Map<String, Field> mockPreprocessedFields = mock(Map.class);
    when(mockPreprocessedFields.get(anyString()))
        .thenAnswer(
            (Answer<Field>)
                invocationOnMock -> {
                  var fieldName = invocationOnMock.getArguments()[0].toString();
                  if (fieldName.contains("invalid")) return null;

                  var split = fieldName.split("\\.");
                  var names = asList(split).subList(1, split.length); // Ignore index name
                  return new Field(mockLinkedMapForField(names));
                });
    when(mockPreprocessedFields.containsKey(anyString()))
        .thenAnswer(
            invocationOnMock -> !invocationOnMock.getArguments()[0].toString().contains("invalid"));

    var fieldCache = new FieldCache(new FieldFactory(), new ESSortListener());
    setInternalState(fieldCache, "validFields", mockPreprocessedFields);
    setInternalState(fieldCache, "indexSettings", indexSettings);
    return fieldCache;
  }

  public static SearchAlias searchAliasFixture() {
    var indexSettings = mock(IndexSettings.class);
    when(indexSettings.getIndex()).thenReturn(INDEX_NAME);

    Map<String, SearchAliasProp> mockSearchAlias = mock(Map.class);
    var props = new HashMap<String, Object>();
    props.put("fields", Map.of("field_before_alias", "field_after_alias"));
    props.put("indices", Map.of("index_before_alias", "index_after_alias"));

    var searchAliasProps = convertValue(props, SearchAliasProp.class);

    when(mockSearchAlias.get(anyString())).thenReturn(searchAliasProps);

    var searchAlias = new SearchAlias(indexSettings);
    setInternalState(searchAlias, "aliases", mockSearchAlias);
    return searchAlias;
  }

  private static LinkedMap mockLinkedMapForField(List<String> names) {
    return rangeClosed(1, names.size())
        .boxed()
        .map(i -> names.stream().limit(i).collect(joining(".")))
        .collect(
            LinkedMap::new,
            (map, field) -> map.put(field, getTypeForField(field)),
            LinkedMap::putAll);
  }

  private static String getTypeForField(String fieldName) {
    if (fieldName.contains("nested")) return "nested";
    if (fieldName.contains("keyword")) return "keyword";
    if (fieldName.contains("geo_point")) return "geo_point";
    return "_obj";
  }

  public static FieldParser fieldParserFixture() {
    return new FieldParser(new NotParser(), searchAliasFixture(), fieldCacheFixture());
  }

  private static FieldParser fieldParserWithoutValidationFixture() {
    return new FieldParser(new NotParser());
  }
}
