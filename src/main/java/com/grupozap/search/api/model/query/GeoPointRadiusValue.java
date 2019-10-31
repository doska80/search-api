package com.grupozap.search.api.model.query;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

import com.grupozap.search.api.exception.InvalidDistanceException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class GeoPointRadiusValue extends Value {

  public static final Value DEFAULT_RADIUS_DISTANCE_VALUE = new Value("2km");

  private static final Pattern PATTERN = compile("(\\d+)(\\D+)");

  public GeoPointRadiusValue(GeoPointValue geoPointValue, Optional<Value> valueDistance) {
    var distance = DEFAULT_RADIUS_DISTANCE_VALUE;
    if (valueDistance.isPresent()) {
      distance = valueDistance.get();
      validateDistance(distance.value());
    }
    this.contents =
        newArrayList(
            ((Value) geoPointValue.contents().get(0)).value(0),
            ((Value) geoPointValue.contents().get(0)).value(1),
            new Value(distance));
  }

  private void validateDistance(final String distance) {
    var matcher = PATTERN.matcher(distance);

    if (matcher.matches()) {
      var number = matcher.group(1);
      var unit = matcher.group(2);

      if (!isNumber(number)) {
        throw new InvalidDistanceException(distance, UnitDistances.getAllowedUnitDistances());
      }

      if (!UnitDistances.getAllowedUnitDistances().contains(unit)) {
        throw new InvalidDistanceException(distance, UnitDistances.getAllowedUnitDistances());
      }
    } else {
      throw new InvalidDistanceException(distance, UnitDistances.getAllowedUnitDistances());
    }
  }

  @Override
  public String toString() {
    return "[" + contents().get(0) + ", " + contents().get(1) + "], " + contents().get(2);
  }

  private enum UnitDistances {
    MILE("mi", "miles"),
    YARD("yd", "yards"),
    FEET("ft", "feet"),
    INCH("in", "inch"),
    KILOMETER("km", "kilometers"),
    METER("m", "meters"),
    CENTIMETER("cm", "centimeters"),
    MILLIMETER("mm", "millimeters"),
    NAUTICAL_MILE("NM", "nmi,", "nauticalmiles");

    private final String[] unitDistances;
    private static final List<String> allowedUnitDistances;

    static {
      allowedUnitDistances = newArrayList();
      Arrays.stream(UnitDistances.values())
          .forEach(
              distancesAllowed ->
                  allowedUnitDistances.addAll(asList(distancesAllowed.unitDistances)));
    }

    UnitDistances(String... unitDistances) {
      this.unitDistances = unitDistances;
    }

    public static List<String> getAllowedUnitDistances() {
      return allowedUnitDistances;
    }
  }
}
