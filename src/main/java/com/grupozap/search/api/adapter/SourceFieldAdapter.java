package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_EXCLUDES;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_INCLUDES;
import static org.apache.commons.lang3.ArrayUtils.contains;

import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.search.Fetchable;
import com.grupozap.search.api.service.parser.factory.FieldCache;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SourceFieldAdapter implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(SourceFieldAdapter.class);

  private static final String FETCH_ALL_FIELD = "*";

  private final FieldCache fieldCache;

  private final Map<String, String[]> defaultSourceIncludes;
  private final Map<String, String[]> defaultSourceExcludes;

  @Autowired
  public SourceFieldAdapter(FieldCache fieldCache) {
    this.fieldCache = fieldCache;

    defaultSourceIncludes = new ConcurrentHashMap<>();
    defaultSourceExcludes = new ConcurrentHashMap<>();
  }

  public void apply(SearchSourceBuilder searchSourceBuilder, final Fetchable request) {
    var includeFields = getFetchSourceIncludeFields(request);
    searchSourceBuilder.fetchSource(
        includeFields, getFetchSourceExcludeFields(request, includeFields));
  }

  public void apply(GetRequest getRequest, final Fetchable request) {
    var includeFields = getFetchSourceIncludeFields(request);
    var fetchSourceContext =
        new FetchSourceContext(
            true, includeFields, getFetchSourceExcludeFields(request, includeFields));
    getRequest.fetchSourceContext(fetchSourceContext);
  }

  private String[] getFetchSourceIncludeFields(final Fetchable request) {
    return request.getIncludeFields() == null
        ? defaultSourceIncludes.getOrDefault(request.getIndex(), new String[] {FETCH_ALL_FIELD})
        : getFetchSourceIncludeFields(request.getIncludeFields(), request.getIndex());
  }

  private String[] getFetchSourceIncludeFields(Set<String> fields, String indexName) {
    return SOURCE_INCLUDES.getValue(fields, indexName).stream()
        .filter(field -> isValidFetchSourceField(indexName, field))
        .toArray(String[]::new);
  }

  private String[] getDefaultFetchSourceExcludeFieldsForIndex(
      String index, String[] defaultIncludes) {
    return getFetchSourceExcludeFields(null, defaultIncludes, index);
  }

  private String[] getFetchSourceExcludeFields(final Fetchable request, String[] includeFields) {
    return request.getExcludeFields() == null && includeFields.length == 0
        ? defaultSourceExcludes.getOrDefault(request.getIndex(), new String[] {})
        : getFetchSourceExcludeFields(
            request.getExcludeFields(), includeFields, request.getIndex());
  }

  private String[] getFetchSourceExcludeFields(
      Set<String> fields, String[] includeFields, String indexName) {
    return SOURCE_EXCLUDES.getValue(fields, indexName).stream()
        .filter(
            field -> !contains(includeFields, field) && isValidFetchSourceField(indexName, field))
        .toArray(String[]::new);
  }

  private String[] getDefaultFetchSourceIncludeFieldsForIndex(String index) {
    return getFetchSourceIncludeFields(null, index);
  }

  private boolean isValidFetchSourceField(String index, String fieldName) {
    if (FETCH_ALL_FIELD.equals(fieldName) || fieldCache.isIndexHasField(index, fieldName)) {
      return true;
    }
    throw new InvalidFieldException(fieldName, index);
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    try {
      var defaultIncludes = getDefaultFetchSourceIncludeFieldsForIndex(event.getIndex());
      defaultSourceIncludes.put(event.getIndex(), defaultIncludes);
      defaultSourceExcludes.put(
          event.getIndex(),
          getDefaultFetchSourceExcludeFieldsForIndex(event.getIndex(), defaultIncludes));
      LOG.debug("Refreshed default source fields for index: " + event.getIndex());
    } catch (InvalidFieldException e) {
      LOG.error("Cannot refresh properties for index: " + event.getIndex(), e);
    }
  }
}
