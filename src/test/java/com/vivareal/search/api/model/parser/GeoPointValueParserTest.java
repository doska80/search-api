package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.vivareal.search.api.model.query.Value;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeoPointValueParserTest {

    private static final Parser<Value> parserViewport = ValueParser.GeoPoint.get(Type.VIEWPORT);
    private static final Parser<Value> parserPolygon = ValueParser.GeoPoint.get(Type.POLYGON);

    @Test
    public void testPolygon() {
        Value parse = parserPolygon.parse("[[42.0,-74.0],[-40.0,-72.0],[-30,-23]]");
        assertEquals("[[42.0, -74.0], [-40.0, -72.0], [-30.0, -23.0]]", parse.toString());
    }

    @Test(expected = ParserException.class)
    public void testPolygonWithLessThan3Points() {
        parserPolygon.parse("[[42.0,-74.0],[-40.0,-72.0]]");
    }

    @Test(expected = ParserException.class)
    public void testPolygonWithInvalidPointLeft() {
        parserPolygon.parse("[[,-74.0],[-40.0,-72.0],[-30,-23]]");
    }

    @Test(expected = ParserException.class)
    public void testPolygonWithInvalidPointRigth() {
        parserPolygon.parse("[[42.0,-74.0],[-40.0,-72.0],[-30,]]");
    }

    @Test
    public void testViewport() {
        Value parse = parserViewport.parse("[[42.0,-74.0],[-40.0,-72.0]]");
        assertEquals("[[42.0, -74.0], [-40.0, -72.0]]", parse.toString());
    }
}
