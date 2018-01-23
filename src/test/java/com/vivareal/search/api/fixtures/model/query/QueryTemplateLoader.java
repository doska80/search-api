package com.vivareal.search.api.fixtures.model.query;

import static com.vivareal.search.api.model.query.RelationalOperator.EQUAL;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.model.query.Filter;
import com.vivareal.search.api.model.query.QueryFragmentItem;
import com.vivareal.search.api.model.query.Value;
import org.apache.commons.collections.map.LinkedMap;

public class QueryTemplateLoader implements TemplateLoader {

  @Override
  public void load() {
    Fixture.of(Field.class)
        .addTemplate(
            "field",
            new Rule() {
              {
                add(
                    "typesByName",
                    new LinkedMap() {
                      {
                        put("field", "_obj");
                      }
                    });
              }
            });

    Fixture.of(Field.class)
        .addTemplate(
            "nested",
            new Rule() {
              {
                add(
                    "typesByName",
                    new LinkedMap() {
                      {
                        put("field1", "_obj");
                        put("field2", "int");
                        put("field3", "int");
                      }
                    });
              }
            });

    Fixture.of(Value.class)
        .addTemplate(
            "value",
            new Rule() {
              {
                add("contents", singletonList("value"));
              }
            });

    Fixture.of(Value.class)
        .addTemplate(
            "null",
            new Rule() {
              {
                add("contents", singletonList(null));
              }
            });

    Fixture.of(Value.class)
        .addTemplate(
            "multiple",
            new Rule() {
              {
                add("names", asList("value1", "value2", "value3"));
              }
            });

    Fixture.of(Filter.class)
        .addTemplate(
            "filter",
            new Rule() {
              {
                add("field", one(Field.class, "field"));
                add("relationalOperator", EQUAL);
                add("value", one(Value.class, "value"));
              }
            });

    Fixture.of(QueryFragmentItem.class)
        .addTemplate(
            "qfi",
            new Rule() {
              {
                add("filter", one(Filter.class, "filter"));
              }
            });
  }
}
