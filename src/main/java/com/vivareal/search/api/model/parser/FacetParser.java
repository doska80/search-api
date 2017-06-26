package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;
import org.jparsec.Scanners;

import java.util.List;

public class FacetParser {

    private static final Parser<List<Field>> FACET_PARSER = FieldParser.getWithoutNot().sepBy1(Scanners.isChar(',').next(Scanners.WHITESPACES.skipMany()));

    public static Parser<List<Field>> get() {
        return FACET_PARSER;
    }
}
