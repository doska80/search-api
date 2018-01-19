package com.vivareal.search.api.model.query;

import static java.lang.String.format;
import static org.apache.lucene.geo.GeoUtils.checkLatitude;
import static org.apache.lucene.geo.GeoUtils.checkLongitude;

import com.vivareal.search.api.model.parser.ValueParser;
import java.util.ArrayList;
import java.util.List;

public class GeoPointValue extends Value {

  public GeoPointValue(List<Value> points, final ValueParser.GeoPoint.Type type) {
    this.checkSize(points, type);
    this.checkCoordinates(points, type);

    this.contents = new ArrayList<>(points);
  }

  private void checkSize(final List<Value> points, final ValueParser.GeoPoint.Type type) {

    if (type.getMinSize() == type.getMaxSize()) {
      if (points.size() != type.getMinSize())
        throw new IllegalArgumentException(
            format("Search by %s need of %s geo_points", type.name(), type.getMinSize()));
    } else {
      if (points.size() < type.getMinSize())
        throw new IllegalArgumentException(
            format("Search by %s need at least %s geo_points", type.name(), type.getMinSize()));

      if (points.size() > type.getMaxSize())
        throw new IllegalArgumentException(
            format(
                "Search by %s cannot greater than %s geo_points", type.name(), type.getMinSize()));
    }
  }

  private void checkCoordinates(final List<Value> points, final ValueParser.GeoPoint.Type type) {
    points
        .parallelStream()
        .forEach(
            point -> {
              if (point == null || point.contents() == null || point.contents().size() != 2)
                throw new IllegalArgumentException(
                    format(
                        "Each geo_point of search by %s should be compose by pair of coordinates (lat/lon)",
                        type.name()));

              checkLatitude(point.value(1));
              checkLongitude(point.value(0));
            });
  }
}
