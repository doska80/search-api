package com.vivareal.search.api.parser;

import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldParserTest {

    @Test
    public void testValidFieldNames() {
        String[] fieldNames = new String[]{
                "field",
                "fieldCamelCase",
                "field1CamelCase2With3Numbers"
        };
        Parser<Field> parser = FieldParser.get();
        for (String fieldName : fieldNames) {
            Field parsedField = parser.parse(fieldName);
            assertEquals(parsedField.getName(), fieldName);
        }
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
        FieldParser.get().parse(".bixola.lolo");
    }

    @Test
    public void testNestedFieldNames() {
        Field field = FieldParser.get().parse("marcos.bixola.lolo");
        assertEquals(field.getName(), "marcos.bixola.lolo");
    }

    @Test(expected = ParserException.class)
    public void testDoublePointFieldNames() {
        FieldParser.get().parse("marcos..bixola");
    }

    @Test(expected = ParserException.class)
    public void testDotEndedFieldNames() {
        FieldParser.get().parse("marcos.");
    }

    @Test(expected = ParserException.class)
    public void testInvalidFieldNamesWithSpecialChars() {
        FieldParser.get().parse("açéntedFïeld");
    }

}