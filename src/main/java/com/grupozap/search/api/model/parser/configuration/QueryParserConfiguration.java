package com.grupozap.search.api.model.parser.configuration;

import com.grupozap.search.api.model.parser.FieldParser;
import com.grupozap.search.api.model.parser.FilterParser;
import com.grupozap.search.api.model.parser.NotParser;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.QueryParser;
import com.grupozap.search.api.model.parser.ValueParser;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.service.parser.factory.FieldCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QueryParserConfiguration {

  @Bean("fieldParser")
  @Primary
  public FieldParser fieldParserUsingFactory(NotParser notParser, FieldCache fieldCache) {
    return new FieldParser(notParser, fieldCache);
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
