package com.grupozap.search.api.model.event;

import org.springframework.context.ApplicationEvent;

public class RemotePropertiesUpdatedEvent extends ApplicationEvent {

  private final String index;

  public RemotePropertiesUpdatedEvent(Object source, String index) {
    super(source);
    this.index = index;
  }

  public String getIndex() {
    return index;
  }
}
