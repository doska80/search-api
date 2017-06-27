package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FieldParserTest {

    @Test
    public void testValidFieldNames() {
        String[] fieldNames = new String[]{
                "field",
                "fieldCamelCase",
                "field1CamelCase2With3Numbers"
        };
        Parser<Field> parser = FieldParser.get();
        Stream.of(fieldNames).forEach(fieldName -> {
            Field parsedField = parser.parse(fieldName);
            assertEquals(parsedField.getName(), fieldName);
        });
    }

    @Test(expected = ParserException.class)
    public void testInvalidFieldNamesWithSpaces() {
        FieldParser.get().parse("field with space");
    }

    @Test(expected = ParserException.class)
    public void testBlankFieldNames() {
        FieldParser.get().parse("");
    }

    @Test(expected = ParserException.class)
    public void testDotFieldNames() {
        FieldParser.get().parse(".");
    }

    @Test(expected = ParserException.class)
    public void testRootNestedFieldNames() {
        FieldParser.get().parse(".abc.def");
    }

    @Test
    public void testNestedFieldNames() {
        Field field = FieldParser.get().parse("field.field2.field3");
        assertEquals(field.getName(), "field.field2.field3");
    }

    @Test(expected = ParserException.class)
    public void testDoublePointFieldNames() {
        FieldParser.get().parse("field..field2");
    }

    @Test(expected = ParserException.class)
    public void testDotEndedFieldNames() {
        FieldParser.get().parse("field.");
    }

    @Test(expected = ParserException.class)
    public void testInvalidFieldNamesWithSpecialChars() {
        FieldParser.get().parse("ãçéntêdFïeld");
    }

    @Test
    public void testStringNames() {
        Field field = FieldParser.get().parse("field.field2.field3.field4");
        assertEquals("field.field2.field3.field4", field.toString());
    }

    @Test(expected = ParserException.class)
    public void testFieldNotWithInvalidPointEnding() {
        FieldParser.get().parse("NOT field.");
    }

    @Test(expected = ParserException.class)
    public void testFieldNotWithInvalidPoint() {
        FieldParser.get().parse("NOT .");
    }


    @Test(expected = ParserException.class)
    public void testNotWithBlankFieldName() {
        FieldParser.get().parse("NOT ");
    }
}
