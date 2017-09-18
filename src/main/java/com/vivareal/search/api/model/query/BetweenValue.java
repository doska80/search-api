package com.vivareal.search.api.model.query;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class BetweenValue extends Value {

    public BetweenValue(Value content) {
        super(getValues(content.getContents()));
    }

    private static Object[] getValues(final List<Object> values) {
        if (isEmpty(values))
            throw new IllegalArgumentException("BETWEEN Query cannot be null or empty. Correct utilization is: filter=fieldName BETWEEN [from,to]");

        if (values.size() != 2)
            throw new IllegalArgumentException("BETWEEN Query need two array positions. Correct utilization is: filter=fieldName BETWEEN [from,to]");

        return new Object[]{((Value) values.get(0)).getContents(0), ((Value) values.get(1)).getContents(0)};
    }

}
