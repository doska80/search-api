package com.grupozap.search.api.model.parser.configuration;

import com.grupozap.search.api.model.parser.FieldParser;
import com.grupozap.search.api.model.parser.FilterParser;
import com.grupozap.search.api.model.parser.NotParser;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.parser.ValueParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryParserWithoutValidationConfiguration {

  @Bean("fieldParserWithoutValidation")
  public FieldParser fieldParserWithoutValidation(NotParser notParser) {
    return new FieldParser(notParser);
  }

  @Bean("filterParserWithoutValidation")
  public FilterParser filterParserWithoutValidation(
      @Qualifier("fieldParserWithoutValidation") FieldParser fieldParser,
      OperatorParser operatorParser,
      ValueParser valueParser) {
    return new FilterParser(fieldParser, operatorParser, valueParser);
  }

  @Bean("queryParserWithoutValidation")
  public QueryParser queryParserWithoutValidation(
      OperatorParser operatorParser,
      @Qualifier("filterParserWithoutValidation") FilterParser filterParser,
      NotParser notParser) {
    return new QueryParser(operatorParser, filterParser, notParser);
  }
}
