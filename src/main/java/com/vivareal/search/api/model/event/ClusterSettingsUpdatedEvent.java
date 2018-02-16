package com.vivareal.search.api.model.event;

import java.util.Map;
import org.springframework.context.ApplicationEvent;

public class ClusterSettingsUpdatedEvent extends ApplicationEvent {

  private final Map<String, Map<String, Object>> settingsByIndex;

  public ClusterSettingsUpdatedEvent(
      Object source, Map<String, Map<String, Object>> settingsByIndex) {
    super(source);

    this.settingsByIndex = settingsByIndex;
  }

  public Map<String, Map<String, Object>> getSettingsByIndex() {
    return settingsByIndex;
  }
}
