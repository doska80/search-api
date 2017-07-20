package com.vivareal.search.api.model.search;

public interface Pageable extends Indexable {
    Integer getFrom();
    void setFrom(Integer from);

    Integer getSize();
    void setSize(Integer size);

    default void setPaginationValues(int defaultSize, int maxSize) {
        setFrom(getFrom() != null && getFrom() >= 0 ? getFrom() : 0);
        setSize(getSize() != null && getSize() >= 0 && getSize() <= maxSize ? getSize() : defaultSize);
    }
}
