package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.LogicalOperator;
import com.vivareal.search.api.model.query.RelationalOperator;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.List;

import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.EQUAL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OperatorParserTest {

    private static Parser<RelationalOperator> relationalParser = OperatorParser.RELATIONAL_OPERATOR_PARSER;
    private static Parser<LogicalOperator> logicalParser = OperatorParser.LOGICAL_OPERATOR_PARSER;

    @Test
    public void testRelationalEqualsOperator() {
        List<RelationalOperator> ops = of(":", "=", "EQ")
                .map(relationalParser::parse)
                .collect(toList());

        assertThat(ops, hasSize(3));
        assertThat(ops, everyItem(equalTo(EQUAL)));
    }

    @Test(expected = ParserException.class)
    public void testRelationalInvalidOperator() {
        relationalParser.parse("?");
    }

    @Test
    public void testLogicalAndOperator() {
        List<LogicalOperator> ops = of("AND", "&&")
                .map(logicalParser::parse)
                .collect(toList());

        assertThat(ops, hasSize(2));
        assertThat(ops, everyItem(equalTo(AND)));
    }

    @Test(expected = ParserException.class)
    public void testLogicalInvalidOperator() {
        logicalParser.parse("?");
    }
}
