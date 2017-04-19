package com.vivareal.search.api.model.query;

public enum Order {
    ASC,
    DESC;

    public Order get(String order) {
        return this.valueOf(order);
    }
}
