package com.vivareal.search.api.fixtures.model.parser;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.HashSet;
import java.util.Set;
import org.mockito.ArgumentMatcher;

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

  public static SortParser sortParserFixture() {
    return new SortParser(fieldParserFixture(), new OperatorParser(), queryParserFixture());
  }

  public static FilterParser filterParserFixture() {
    return new FilterParser(fieldParserFixture(), new OperatorParser(), new ValueParser());
  }

  public static FieldFactory fieldFactoryFixture() {
    ArgumentMatcher<String> notContainsSpecialTypes = notContainsSpecialTypes();

    IndexSettings indexSettings = mock(IndexSettings.class);
    doNothing().when(indexSettings).validateField(argThat(notContainsInvalidType()));
    doThrow(new InvalidFieldException("field", "Expected exception"))
        .when(indexSettings)
        .validateField(argThat(containsInvalidType()));
    when(indexSettings.getFieldType(contains("nested"))).thenReturn("nested");
    when(indexSettings.getFieldType(contains("keyword"))).thenReturn("keyword");
    when(indexSettings.getFieldType(contains("geo_point"))).thenReturn("geo_point");
    when(indexSettings.getFieldType(argThat(notContainsSpecialTypes))).thenReturn("_obj");

    FieldFactory fieldFactory = new FieldFactory();
    fieldFactory.setIndexSettings(indexSettings);
    return fieldFactory;
  }

  public static FieldParser fieldParserFixture() {
    return new FieldParser(new NotParser(), fieldFactoryFixture());
  }

  private static ArgumentMatcher<String> notContainsSpecialTypes() {
    return new ArgumentMatcher<String>() {
      Set<String> specialTypes = new HashSet<>(asList("nested", "keyword", "geo_point"));

      @Override
      public boolean matches(Object o) {
        return !specialTypes.stream().anyMatch(str -> o.toString().contains(str));
      }
    };
  }

  private static ArgumentMatcher<Field> containsInvalidType() {
    return new ArgumentMatcher<Field>() {
      Set<String> invalidFields = new HashSet<>(asList("invalid"));

      @Override
      public boolean matches(Object o) {
        return invalidFields
            .stream()
            .anyMatch(invalidField -> ((Field) o).getName().toString().contains(invalidField));
      }
    };
  }

  private static ArgumentMatcher<Field> notContainsInvalidType() {
    return new ArgumentMatcher<Field>() {
      Set<String> invalidFields = new HashSet<>(asList("invalid"));

      @Override
      public boolean matches(Object o) {
        return !invalidFields
            .stream()
            .anyMatch(invalidField -> ((Field) o).getName().toString().contains(invalidField));
      }
    };
  }
}
