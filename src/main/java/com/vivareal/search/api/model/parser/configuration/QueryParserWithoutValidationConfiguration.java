package com.vivareal.search.api.model.parser.configuration;

import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryParserWithoutValidationConfiguration {

  @Bean("fieldParserWithoutValidation")
  public FieldParser fieldParserWithoutValidation(NotParser notParser, FieldFactory fieldFactory) {
    return new FieldParser(notParser, fieldFactory);
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
