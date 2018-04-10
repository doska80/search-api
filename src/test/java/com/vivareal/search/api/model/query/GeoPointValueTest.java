package com.vivareal.search.api.model.query;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Double.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.lucene.geo.GeoUtils.MIN_LAT_INCL;
import static org.apache.lucene.geo.GeoUtils.MIN_LON_INCL;
import static org.junit.Assert.assertEquals;

import com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type;
import java.util.List;
import org.junit.Test;

public class GeoPointValueTest {

  @Test(expected = IllegalArgumentException.class)
  public void emptyViewport() {
    new GeoPointValue(emptyList(), Type.VIEWPORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptySingle() {
    new GeoPointValue(emptyList(), Type.SINGLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void hugeViewport() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(1.0), new Value(2.0))),
            new Value(asList(new Value(3.0), new Value(4.0))),
            new Value(asList(new Value(5.0), new Value(6.0))));

    new GeoPointValue(viewport, Type.VIEWPORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void hugeSingle() {
    List<Value> single =
        newArrayList(
            new Value(asList(new Value(30.0), new Value(-40.0))),
            new Value(asList(new Value(40.0), new Value(-50.0))));

    new GeoPointValue(single, Type.SINGLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullViewport() {
    List<Value> viewport = newArrayList(new Value(asList(new Value(1.0), new Value(2.0))), null);

    new GeoPointValue(viewport, Type.VIEWPORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullSingle() {
    List<Value> single = newArrayList(new Value(asList(new Value(30.0), new Value(-40.0))), null);

    new GeoPointValue(single, Type.SINGLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void hugeItemViewport() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(1.0), new Value(2.0))),
            new Value(asList(new Value(3.0), new Value(4.0), new Value(5.0))));

    new GeoPointValue(viewport, Type.VIEWPORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void hugeItemSingle() {
    List<Value> single =
        newArrayList(
            new Value(asList(new Value(30.0), new Value(-40.0))),
            new Value(asList(new Value(40.0), new Value(-50.0), new Value(50.0))));

    new GeoPointValue(single, Type.SINGLE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidViewportLatitude() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(MIN_LAT_INCL - 100), new Value(2.0))),
            new Value(asList(new Value(3.3), new Value(4.0))));

    new GeoPointValue(viewport, Type.VIEWPORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidViewportLongitude() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(-1.0), new Value(2.0))),
            new Value(asList(new Value(MIN_LON_INCL - 100), new Value(4.0))));

    new GeoPointValue(viewport, Type.VIEWPORT);
  }

  @Test
  public void validViewport() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(10.1), new Value(2.0))),
            new Value(asList(new Value(3.3), new Value(4.0))));

    GeoPointValue viewportValue = new GeoPointValue(viewport, Type.VIEWPORT);

    assertEquals(valueOf(10.1), viewportValue.value(0, 0));
    assertEquals(valueOf(2.0), viewportValue.value(0, 1));
    assertEquals(valueOf(3.3), viewportValue.value(1, 0));
    assertEquals(valueOf(4.0), viewportValue.value(1, 1));
  }

  @Test
  public void validPolygon() {
    List<Value> viewport =
        newArrayList(
            new Value(asList(new Value(10.1), new Value(2.0))),
            new Value(asList(new Value(3.3), new Value(4.0))),
            new Value(asList(new Value(5.5), new Value(6.1))));

    GeoPointValue polygon = new GeoPointValue(viewport, Type.POLYGON);

    assertEquals(valueOf(10.1), polygon.value(0, 0));
    assertEquals(valueOf(2.0), polygon.value(0, 1));
    assertEquals(valueOf(3.3), polygon.value(1, 0));
    assertEquals(valueOf(4.0), polygon.value(1, 1));
    assertEquals(valueOf(5.5), polygon.value(2, 0));
    assertEquals(valueOf(6.1), polygon.value(2, 1));
  }

  @Test
  public void validSingle() {
    List<Value> single = newArrayList(new Value(asList(new Value(30.0), new Value(-40.0))));

    GeoPointValue polygon = new GeoPointValue(single, Type.SINGLE);

    assertEquals(valueOf(30.0), polygon.value(0, 0));
    assertEquals(valueOf(-40.0), polygon.value(0, 1));
  }
}
