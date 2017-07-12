package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;

import java.util.List;

import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;

public class FacetParser {

    private static final Parser<List<Field>> FACET_PARSER = FieldParser.getWithoutNot().sepBy1(isChar(',').next(WHITESPACES.skipMany())).label("multiple fields");

    public static Parser<List<Field>> get() {
        return FACET_PARSER;
    }
}
