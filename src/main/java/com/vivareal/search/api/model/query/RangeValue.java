package com.vivareal.search.api.model.query;

public class RangeValue extends Value {

    public RangeValue(Value content) {
        super(content.getContents());

        if (content.getContents() == null || content.getContents().size() != 2)
            throw new IllegalArgumentException("The RANGE filter does not have from/to pair");
    }

}
