package com.vivareal.search.api.adapter;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractQueryAdapter {

    protected static final ImmutableList<Field> EMPTY_LIST = ImmutableList.of();
    protected static final Pattern FIELD_VALUES = Pattern.compile("\\s*(\\w+)(:|>=|<=|>|<)\\s*(?:\")?(.*?(?=\"?\\s+\\w+(:|>=|<=|>|<)|(?:\"?)$))");

    public List<Field> parseQuery(final String query) {
        Matcher m = FIELD_VALUES.matcher(query);

        boolean found = m.find();
        if (!found)
            return EMPTY_LIST;

        ImmutableList.Builder<Field> fieldListBuilder = ImmutableList.builder();
        do {
            fieldListBuilder.add(new Field(m.group(1), m.group(2), m.group(3)));
        } while (m.find());

        return fieldListBuilder.build();
    }

    public class Field {

        public String name;
        public Expression expression;
        public Object value;

        protected Field(String name, String expression, String value) {
            this(name, Expression.get(expression), value);
        }

        protected Field(String name, Expression expression, String value) {
            this.setName(name);
            this.setExpression(expression);
            this.setValue(value);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Expression getExpression() {
            return expression;
        }

        public void setExpression(Expression expression) {
            this.expression = expression;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public enum Expression {
        //        DIFFERENT("!:"),
        EQUAL(":"),
        GREATER(">"),
        GREATER_EQUAL(">="),
        LESS("<"),
        LESS_EQUAL("<=");

        private String expr;

        Expression(String expr) {
            this.expr = expr;
        }

        public static Expression get(String expression) { // FIXME this is going to be slow
            for (Expression e : Expression.values()) {
                if (e.expr.equals(expression)) return e;
            }
            throw new IllegalArgumentException("Expression \"" + expression + "\" not found!");
        }

    }

}
