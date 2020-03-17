package com.grupozap.search.api.model.listener.script;

import static com.grupozap.search.api.utils.MapperUtils.convertValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScriptField {
  private String id;
  private String scriptType;
  private String scriptSortType;
  private String lang;
  private Map<String, Object> params;

  private ScriptField() {}

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

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static List<ScriptField> build(Map<String, Map<String, Object>> entryValue) {
    var scripts = new ArrayList<ScriptField>();
    if (entryValue.containsKey("scripts")) {
      ((List) entryValue.get("scripts"))
          .forEach(scriptField -> scripts.add(convertValue(scriptField, ScriptField.class)));
    }
    return scripts;
  }
}
