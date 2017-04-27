package com.vivareal.search.api.parser;

import com.vivareal.search.api.model.query.Expression;
import org.jparsec.*;

import java.util.List;

public class FilterParser {

    static final Parser<Void> IGNORED = Parsers.or(Scanners.WHITESPACES).skipMany();
    static final Parser<String> STRING_TOKENIZER = Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER.or(Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER);
    static final Terminals TERMINALS = Terminals
            .operators(new String[] { ":", "=", "EQ", "NEQ", "GT", "LT", "GTE", "LTE", "(", ")", "[", "]", ",", "<>" })
            .words(Scanners.IDENTIFIER)
            .keywords(new String[] { "OR", "AND", "NOT", "IN" })
            .build();
    static Parser<Expression> EXPRESSION_PARSER = Parsers.sequence(Terminals.fragment(Tokens.Tag.IDENTIFIER).map(e -> Expression.valueOf(e))).cast();
    static final Parser<?> TOKENIZER = Parsers.or(STRING_TOKENIZER, TERMINALS.tokenizer());
    static final Parser<?> PARSER = Parsers.or(TOKENIZER, IGNORED);

    public static void main(String[] args) {
        System.out.println(TERMINALS.token("OR"));
        List<?> parsed = PARSER.many().parse("field: value AND outroField:outroValue OR bla EQ ble AND bla EQ 'ble AND bli' AND foo='abc' AND bar<>'def' OR (biz IN ['a', 'b', 'c'] AND NOT baz = 'foo'");
        System.out.println(parsed);
    }

}
