package com.vivareal.search.api.model.query;

import static org.springframework.util.CollectionUtils.isEmpty;

import com.google.common.base.Objects;
import java.util.Set;
import org.apache.commons.collections.map.LinkedMap;

public class Field {

  private boolean not;
  private LinkedMap typesByName;

  public Field(LinkedMap typesByName) {
    this(false, typesByName);
  }

  public Field(boolean not, LinkedMap typesByName) {
    if (isEmpty(typesByName)) {
      throw new IllegalArgumentException("The field name cannot be empty");
    }

    this.not = not;
    this.typesByName = typesByName;
  }

  public boolean isNot() {
    return not;
  }

  public LinkedMap getTypesByName() {
    return typesByName;
  }

  public String firstName() {
    return typesByName.firstKey().toString();
  }

  public String getName() {
    return typesByName.lastKey().toString();
  }

  public Set<String> getNames() {
    return typesByName.keySet();
  }

  public String getTypeFirstName() {
    return getType(firstName());
  }

  public String getType() {
    return getType(getName());
  }

  public String getType(String field) {
    return typesByName.get(field).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Field field = (Field) o;

    return Objects.equal(this.typesByName, field.typesByName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.typesByName);
  }

  @Override
  public String toString() {
    return getName();
  }
}
