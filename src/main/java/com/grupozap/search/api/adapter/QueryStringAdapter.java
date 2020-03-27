package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_DEFAULT_FIELDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_MM;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_TEMPLATES;
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
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.model.search.Queryable;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  public static final float DEFAULT_BOOST_VALUE = 1.0f;
  public static final int DEFAULT_MAX_EXPANSIONS = 5;
  public static final int DEFAULT_SLOP = 2;
  private static final String NOT_NESTED = "not_nested";
  private static final String MM_ERROR_MESSAGE =
      "Minimum Should Match (mm) should be a valid integer number (-100 <> +100)";
  private static final Set<QSTemplate> DEFAULT_QS_TEMPLATE = singleton(new QSTemplate());

  private final FieldCache fieldCache;

  private final Map<String, Set<QSTemplate>> queryTemplatePerIndex;

  public QueryStringAdapter(FieldCache fieldCache) {
    this.fieldCache = fieldCache;
    this.queryTemplatePerIndex = new ConcurrentHashMap<>();
  }

  private static QSField createQSField(String[] boostFieldValues) {
    return new QSField(
        boostFieldValues[0],
        boostFieldValues.length == 2 ? parseFloat(boostFieldValues[1]) : DEFAULT_BOOST_VALUE);
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
                            var maxExpansion = qsTemplate.getMaxExpansions();
                            var slop = qsTemplate.getSlop();
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
                                    mm,
                                    maxExpansion,
                                    slop);
                              else
                                queryStringQueries.put(
                                    nestedField,
                                    nestedQuery(
                                        nestedField,
                                        buildQueryStringQuery(
                                            null,
                                            qsTemplate,
                                            request.getQ(),
                                            field,
                                            boost,
                                            mm,
                                            maxExpansion,
                                            slop),
                                        None));
                            } else {
                              if (queryStringQueries.containsKey(NOT_NESTED))
                                buildQueryStringQuery(
                                    (MultiMatchQueryBuilder) queryStringQueries.get(NOT_NESTED),
                                    qsTemplate,
                                    request.getQ(),
                                    field,
                                    boost,
                                    mm,
                                    maxExpansion,
                                    slop);
                              else
                                queryStringQueries.put(
                                    NOT_NESTED,
                                    buildQueryStringQuery(
                                        null,
                                        qsTemplate,
                                        request.getQ(),
                                        field,
                                        boost,
                                        mm,
                                        maxExpansion,
                                        slop));
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
      final String mm,
      final int maxExpansions,
      final int slop) {
    if (multiMatchQueryBuilder == null) multiMatchQueryBuilder = multiMatchQuery(q);
    multiMatchQueryBuilder
        .field(field.getName(), fieldBoost)
        .minimumShouldMatch(mm)
        .tieBreaker(0.2f)
        .type(qsTemplate.getType())
        .boost(qsTemplate.getBoost())
        .maxExpansions(maxExpansions)
        .slop(slop);
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
    private float boost;
    private int maxExpansions;
    private int slop;
    private Map<String, List<QSField>> fieldAliases;

    public QSTemplate() {
      this.type = BEST_FIELDS;
      this.boost = DEFAULT_BOOST_VALUE;
      this.maxExpansions = DEFAULT_MAX_EXPANSIONS;
      this.slop = DEFAULT_SLOP;
      this.fieldAliases = new HashMap<>();
    }

    public Type getType() {
      return type;
    }

    public void setType(String type) {
      this.type = Type.valueOf(type);
    }

    public float getBoost() {
      return boost;
    }

    public void setBoost(float boost) {
      this.boost = boost;
    }

    public int getMaxExpansions() {
      return maxExpansions;
    }

    public void setMaxExpansions(int maxExpansions) {
      this.maxExpansions = maxExpansions;
    }

    public int getSlop() {
      return slop;
    }

    public void setSlop(int slop) {
      this.slop = slop;
    }

    public Map<String, List<QSField>> getFieldAliases() {
      return fieldAliases;
    }

    public void setFieldAliases(Map<String, List<?>> fieldAliases) {
      Map<String, List<QSField>> qsFieldAliases = new HashMap<>();
      ofNullable(fieldAliases)
          .orElseGet(HashMap::new)
          .forEach(
              (alias, fields) ->
                  qsFieldAliases.put(
                      alias,
                      ofNullable(fields).orElseGet(ArrayList::new).stream()
                          .filter(Objects::nonNull)
                          .map(this::toQSField)
                          .collect(toList())));
      this.fieldAliases = qsFieldAliases;
    }

    private QSField toQSField(Object obj) {
      if (obj instanceof QSField) {
        return (QSField) obj;
      }
      if (obj instanceof String) {
        return createQSField(((String) obj).split(":"));
      }
      if (obj instanceof Map) {
        final var qsMap = (Map) obj;
        return new QSField(
            (String) qsMap.getOrDefault("fieldName", ""),
            (Float) qsMap.getOrDefault("boost", DEFAULT_BOOST_VALUE));
      }
      throw new RuntimeException("invalid QSField: " + obj.getClass().getName());
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
