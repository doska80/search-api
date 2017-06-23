package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

import java.util.List;

import static org.jparsec.Scanners.isChar;

public class SortParser {
    private static final Parser<Sort> SORT_PARSER = Parsers.sequence(FieldParser.get(), OperatorParser.ORDER_OPERATOR_PARSER.optional(OrderOperator.ASC), Sort::new).sepBy(Scanners.isChar(',').next(Scanners.WHITESPACES.skipMany())).map(Sort::new);

    public static Parser<Sort> get() {
        return SORT_PARSER;
    }
}
