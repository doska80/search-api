package com.vivareal.search.api.model.search;

import static java.lang.String.format;

public interface Pageable extends Indexable {
    int getFrom();
    void setFrom(int from);

    int getSize();
    void setSize(int size);

    default void setPaginationValues(final int defaultSize, final int maxSize) {
        if (getFrom() < 0)
            throw new IllegalArgumentException("Parameter [from] must be a positive integer");

        setFrom(getFrom());

        if(getSize() != Integer.MAX_VALUE) {
            if (getSize() < 0 || getSize() > maxSize)
                throw new IllegalArgumentException(format("Parameter [size] must be a positive integer less than %d (default: %d)", maxSize, defaultSize));

            setSize(getSize());
        } else
            setSize(defaultSize);
    }
}
