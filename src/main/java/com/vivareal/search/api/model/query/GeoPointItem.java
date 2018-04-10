package com.vivareal.search.api.model.query;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;

import com.google.common.base.Objects;
import java.util.Optional;

public class GeoPointItem extends Item {
  private GeoPointValue geoPointValue;

  public GeoPointItem(
      Field field, GeoPointValue geoPointValue, Optional<QueryFragment> queryFragment) {
    this.setField(field);
    this.setOrderOperator(OrderOperator.ASC);
    this.geoPointValue = geoPointValue;
    this.setQueryFragment(queryFragment);
  }

  public GeoPointValue getGeoPointValue() {
    return geoPointValue;
  }

  @Override
  public String toString() {
    return format(
            "%s %s %s %s",
            this.getField(),
            this.getOrderOperator(),
            this.geoPointValue,
            this.getQueryFragment().map(QueryFragment::toString).orElse(EMPTY))
        .trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GeoPointItem item = (GeoPointItem) o;
    return equal(this.getField(), item.getField())
        && equal(this.getOrderOperator(), item.getOrderOperator())
        && equal(this.getQueryFragment(), item.getQueryFragment())
        && equal(this.geoPointValue, item.geoPointValue)
        && equal(this.getQueryFragment(), item.getQueryFragment());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        this.getField(), this.getOrderOperator(), this.geoPointValue, this.getQueryFragment());
  }
}
