package com.grupozap.search.api.service.parser.factory;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_ALIAS;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static java.util.Objects.nonNull;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.service.parser.IndexSettings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("searchAlias")
public class SearchAlias implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(SearchAlias.class);

  private final IndexSettings indexSettings;
  private Map<String, SearchAliasProp> aliases;

  @Autowired
  public SearchAlias(IndexSettings indexSettings) {
    this.indexSettings = indexSettings;
    this.aliases = new ConcurrentHashMap<>();
  }

  public Map<String, SearchAliasProp> getAliases() {
    return this.aliases;
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    Object value = ES_ALIAS.getValue(event.getIndex());
    if (nonNull(value)) {
      this.aliases.put(event.getIndex(), convertValue(value, SearchAliasProp.class));
      LOG.debug("Refreshing es aliases :." + this.aliases.toString());
    } else {
      this.aliases.put(event.getIndex(), new SearchAliasProp());
    }
  }

  public String getFieldAlias(String fieldName) {
    return this.aliases
        .get(indexSettings.getIndex())
        .getFields()
        .getOrDefault(fieldName, fieldName);
  }

  public String getIndexAlias(String indexName) {
    return this.aliases
        .get(indexSettings.getIndex())
        .getIndices()
        .getOrDefault(indexName, indexName);
  }

  public static final class SearchAliasProp {
    private Map<String, String> fields;
    private Map<String, String> indices;

    SearchAliasProp() {
      this.fields = new ConcurrentHashMap<>();
      this.indices = new ConcurrentHashMap<>();
    }

    public Map<String, String> getFields() {
      return fields;
    }

    public Map<String, String> getIndices() {
      return indices;
    }
  }
}
