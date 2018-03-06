package com.vivareal.search.api.model.parser.configuration;

import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.service.parser.factory.FieldCache;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QueryParserConfiguration {

  @Bean("fieldParser")
  @Primary
  public FieldParser fieldParserUsingFactory(
      NotParser notParser, FieldFactory fieldFactory, FieldCache fieldCache) {
    return new FieldParser(notParser, fieldFactory, fieldCache);
  }

  @Bean("filterParser")
  @Primary
  public FilterParser filterParserUsingFactory(
      @Qualifier("fieldParser") FieldParser fieldParser,
      OperatorParser operatorParser,
      ValueParser valueParser) {
    return new FilterParser(fieldParser, operatorParser, valueParser);
  }

  @Bean("queryParser")
  @Primary
  public QueryParser queryParserUsingFactory(
      OperatorParser operatorParser,
      @Qualifier("filterParser") FilterParser filterParser,
      NotParser notParser) {
    return new QueryParser(operatorParser, filterParser, notParser);
  }
}
