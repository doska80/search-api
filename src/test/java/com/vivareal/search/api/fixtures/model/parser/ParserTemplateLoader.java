package com.vivareal.search.api.fixtures.model.parser;

import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LinkedMap;
import org.mockito.stubbing.Answer;

public class ParserTemplateLoader {

  public static QueryParser queryParserFixture() {
    OperatorParser operatorParser = new OperatorParser();
    FieldParser fieldParser = fieldParserFixture();
    FilterParser filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    return new QueryParser(operatorParser, filterParser, new NotParser());
  }

  public static FacetParser facetParserFixture() {
    return new FacetParser(fieldParserFixture());
  }

  public static FilterParser filterParserFixture() {
    return new FilterParser(fieldParserFixture(), new OperatorParser(), new ValueParser());
  }

  public static FieldFactory fieldFactoryFixture() {
    IndexSettings indexSettings = mock(IndexSettings.class);
    when(indexSettings.getIndex()).thenReturn(INDEX_NAME);

    Map<String, Field> mockPreprocessedFields = mock(Map.class);
    when(mockPreprocessedFields.get(anyString()))
        .thenAnswer(
            (Answer<Field>)
                invocationOnMock -> {
                  String fieldName = invocationOnMock.getArguments()[0].toString();
                  if (fieldName.contains("invalid")) return null;

                  String[] split = fieldName.split("\\.");
                  List<String> names = asList(split).subList(1, split.length); // Ignore index name
                  return new Field(mockLinkedMapForField(names));
                });

    FieldFactory fieldFactory = new FieldFactory();
    setInternalState(fieldFactory, "validFields", mockPreprocessedFields);
    setInternalState(fieldFactory, "indexSettings", indexSettings);
    return fieldFactory;
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
    return new FieldParser(new NotParser(), fieldFactoryFixture());
  }
}
