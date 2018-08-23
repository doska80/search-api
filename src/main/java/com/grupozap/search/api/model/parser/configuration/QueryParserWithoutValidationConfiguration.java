package com.grupozap.search.api.model.parser.configuration;

import com.grupozap.search.api.model.parser.*;
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
