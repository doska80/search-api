package com.grupozap.search.api.listener;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SCRIPTS;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import java.util.*;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ScriptRemotePropertiesListener
    implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private Map<String, Set<ScriptField>> scripts;

  public ScriptRemotePropertiesListener() {
    this.scripts = new HashMap<>();
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    List<Object> value = ES_SCRIPTS.getValue(event.getIndex());
    if (!isEmpty(value)) {
      final Set<ScriptField> indexScript = new HashSet<>();
      value.forEach(v -> indexScript.add(toScriptField(v)));
      this.scripts.put(event.getIndex(), indexScript);
    } else {
      this.scripts.put(event.getIndex(), new HashSet<>());
    }
  }

  public Map<String, Set<ScriptField>> getScripts() {
    return this.scripts;
  }

  private ScriptField toScriptField(Object obj) {
    return convertValue(obj, ScriptField.class);
  }

  public static class ScriptField {
    private String id;
    private String scriptType;
    private String scriptSortType;
    private String lang;
    private Map<String, Object> params;

    public String getId() {
      return id;
    }

    public String getScriptType() {
      return scriptType;
    }

    public String getScriptSortType() {
      return scriptSortType;
    }

    public String getLang() {
      return lang;
    }

    public Map<String, Object> getParams() {
      return params;
    }
  }
}
