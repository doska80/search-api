package com.grupozap.search.api.fixtures.model.query;

import static com.grupozap.search.api.model.query.LogicalOperator.AND;
import static com.grupozap.search.api.model.query.RelationalOperator.EQUAL;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.model.query.Filter;
import com.grupozap.search.api.model.query.QueryFragmentItem;
import com.grupozap.search.api.model.query.Value;
import org.apache.commons.collections4.map.LinkedMap;

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
            "multiple",
            new Rule() {
              {
                add(
                    "typesByName",
                    new LinkedMap() {
                      {
                        put("multiple", "keyword");
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
                        put("field1.field2", "int");
                        put("field1.field2.field3", "int");
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
                add("contents", asList("value1", "value2", "value3"));
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

    Fixture.of(Filter.class)
        .addTemplate(
            "multiple",
            new Rule() {
              {
                add("field", one(Field.class, "multiple"));
                add("relationalOperator", EQUAL);
                add("value", one(Value.class, "multiple"));
              }
            });

    Fixture.of(Filter.class)
        .addTemplate(
            "nested",
            new Rule() {
              {
                add("field", one(Field.class, "nested"));
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

    Fixture.of(QueryFragmentItem.class)
        .addTemplate(
            "qfiMultiple",
            new Rule() {
              {
                add("filter", one(Filter.class, "multiple"));
              }
            });

    Fixture.of(QueryFragmentItem.class)
        .addTemplate(
            "qfiNested",
            new Rule() {
              {
                add("logicalOperator", AND);
                add("filter", one(Filter.class, "nested"));
              }
            });
  }
}
