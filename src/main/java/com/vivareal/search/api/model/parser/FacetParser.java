package com.vivareal.search.api.model.parser;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.query.Field;
import org.jparsec.Parser;

import java.util.List;

import static org.jparsec.Scanners.WHITESPACES;
import static org.jparsec.Scanners.isChar;

public class FacetParser {

    private static final Parser<List<Field>> FACET_PARSER = FieldParser.getWithoutNot().sepBy1(isChar(',').next(WHITESPACES.skipMany())).label("multiple fields");

    @Trace
    public static List<Field> parse(String string) {
        return FACET_PARSER.parse(string);
    }
}
