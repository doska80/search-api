package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.*;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static java.lang.Float.parseFloat;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS;
import static org.elasticsearch.index.query.QueryBuilders.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.model.search.Queryable;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class QueryStringAdapter implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final String NOT_NESTED = "not_nested";
  private static final String MM_ERROR_MESSAGE =
      "Minimum Should Match (mm) should be a valid integer number (-100 <> +100)";
  private static final float DEFAULT_BOOST_VALUE = 1.0f;

  private static final Set<QSTemplate> DEFAULT_QS_TEMPLATE = singleton(new QSTemplate());

  private final FieldCache fieldCache;

  private final Map<String, Set<QSTemplate>> queryTemplatePerIndex;

  public QueryStringAdapter(FieldCache fieldCache) {
    this.fieldCache = fieldCache;
    this.queryTemplatePerIndex = new ConcurrentHashMap<>();
  }

  public void apply(BoolQueryBuilder queryBuilder, final Queryable request) {
    if (isEmpty(request.getQ())) return;

    var indexName = request.getIndex();

    String mm = isEmpty(request.getMm()) ? QS_MM.getValue(indexName) : request.getMm();
    checkMM(mm);

    var queries =
        queryTemplatePerIndex.getOrDefault(request.getIndex(), DEFAULT_QS_TEMPLATE).stream()
            .map(
                qsTemplate -> {
                  var boolQuery = boolQuery();

                  Map<String, AbstractQueryBuilder> queryStringQueries = new HashMap<>();
                  getQueryStringFields(qsTemplate, request, indexName)
                      .forEach(
                          qsField -> {
                            var field = fieldCache.getField(qsField.getFieldName());
                            var boost = qsField.getBoost();
                            if (FIELD_TYPE_NESTED.typeOf(field.getTypeFirstName())) {
                              var nestedField = field.firstName();

                              if (queryStringQueries.containsKey(nestedField))
                                buildQueryStringQuery(
                                    (MultiMatchQueryBuilder)
                                        ((NestedQueryBuilder) queryStringQueries.get(nestedField))
                                            .query(),
                                    qsTemplate,
                                    request.getQ(),
                                    field,
                                    boost,
                                    mm);
                              else
                                queryStringQueries.put(
                                    nestedField,
                                    nestedQuery(
                                        nestedField,
                                        buildQueryStringQuery(
                                            null, qsTemplate, request.getQ(), field, boost, mm),
                                        None));
                            } else {
                              if (queryStringQueries.containsKey(NOT_NESTED))
                                buildQueryStringQuery(
                                    (MultiMatchQueryBuilder) queryStringQueries.get(NOT_NESTED),
                                    qsTemplate,
                                    request.getQ(),
                                    field,
                                    boost,
                                    mm);
                              else
                                queryStringQueries.put(
                                    NOT_NESTED,
                                    buildQueryStringQuery(
                                        null, qsTemplate, request.getQ(), field, boost, mm));
                            }
                          });

                  queryStringQueries.forEach(
                      (nestedPath, nestedQuery) -> boolQuery.must().add(nestedQuery));
                  return boolQuery;
                })
            .collect(toList());

    if (queries.size() == 1) {
      queryBuilder.must().addAll(queries.get(0).must());
    } else {
      var shouldBetweenQueries = boolQuery();
      queries.forEach(q -> shouldBetweenQueries.should().addAll(q.must()));

      queryBuilder.must().add(shouldBetweenQueries);
    }
  }

  private List<QSField> getQueryStringFields(
      QSTemplate qsTemplate, Queryable request, String indexName) {
    List<QSField> qsFields = new ArrayList<>();
    QS_DEFAULT_FIELDS.getValue(request.getFields(), indexName).stream()
        .map(qsField -> qsField.split(":"))
        .forEach(
            qsRaw -> {
              var aliasFields =
                  qsTemplate.getFieldAliases().getOrDefault(qsRaw[0], new LinkedList<>());
              if (!aliasFields.isEmpty()) {
                qsFields.addAll(aliasFields);
              } else if (!fieldCache.isIndexHasField(indexName, qsRaw[0])) {
                throw new InvalidFieldException(qsRaw[0], indexName);
              } else {
                qsFields.add(createQSField(qsRaw));
              }
            });
    return qsFields;
  }

  private static QSField createQSField(String[] boostFieldValues) {
    return new QSField(
        boostFieldValues[0],
        boostFieldValues.length == 2 ? parseFloat(boostFieldValues[1]) : DEFAULT_BOOST_VALUE);
  }

  private void checkMM(final String mm) {
    if (mm.contains(".") || mm.contains("%") && ((mm.length() - 1) > mm.indexOf('%')))
      throw new NumberFormatException(MM_ERROR_MESSAGE);

    var mmNumber = mm.replace("%", "");

    if (!isCreatable(mmNumber)) throw new NumberFormatException(MM_ERROR_MESSAGE);

    var number = toInt(mmNumber);

    if (number < -100 || number > 100) throw new IllegalArgumentException(MM_ERROR_MESSAGE);
  }

  private MultiMatchQueryBuilder buildQueryStringQuery(
      MultiMatchQueryBuilder multiMatchQueryBuilder,
      QSTemplate qsTemplate,
      final String q,
      Field field,
      float fieldBoost,
      final String mm) {
    if (multiMatchQueryBuilder == null) multiMatchQueryBuilder = multiMatchQuery(q);
    multiMatchQueryBuilder
        .field(field.getName(), fieldBoost)
        .minimumShouldMatch(mm)
        .tieBreaker(0.2f)
        .type(qsTemplate.getType())
        .boost(qsTemplate.getBoost());
    return multiMatchQueryBuilder;
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    var rawQueryTemplates =
        ofNullable((Set) QS_TEMPLATES.getValue(event.getIndex()))
            .filter(queries -> !queries.isEmpty())
            .orElse(DEFAULT_QS_TEMPLATE);

    queryTemplatePerIndex.put(
        event.getIndex(),
        (Set<QSTemplate>)
            rawQueryTemplates.stream()
                .map(this::toQSTemplate)
                .collect(toCollection(() -> new LinkedHashSet<>(rawQueryTemplates.size()))));
  }

  private QSTemplate toQSTemplate(Object rawQueryTemplate) {
    return convertValue(rawQueryTemplate, QSTemplate.class);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class QSTemplate {
    private Type type;
    private Integer boost;
    private Map<String, List<QSField>> fieldAliases;

    public QSTemplate() {
      this.type = BEST_FIELDS;
      this.boost = 1;
      this.fieldAliases = new HashMap<>();
    }

    public Type getType() {
      return type;
    }

    public void setType(String type) {
      this.type = Type.valueOf(type);
    }

    public Integer getBoost() {
      return boost;
    }

    public void setBoost(Integer boost) {
      this.boost = boost;
    }

    public Map<String, List<QSField>> getFieldAliases() {
      return fieldAliases;
    }

    public void setFieldAliases(Map<String, List<String>> fieldAliases) {
      Map<String, List<QSField>> qsFieldAliases = new HashMap<>();
      ofNullable(fieldAliases)
          .orElseGet(HashMap::new)
          .forEach(
              (alias, fieldNames) ->
                  qsFieldAliases.put(
                      alias,
                      ofNullable(fieldNames).orElseGet(ArrayList::new).stream()
                          .map(qs -> qs.split(":"))
                          .map(QueryStringAdapter::createQSField)
                          .collect(toList())));
      this.fieldAliases = qsFieldAliases;
    }
  }

  private static class QSField {
    private final String fieldName;
    private final float boost;

    public QSField(String fieldName, float boost) {
      this.fieldName = fieldName;
      this.boost = boost;
    }

    public String getFieldName() {
      return fieldName;
    }

    public float getBoost() {
      return boost;
    }
  }
}
