package com.vivareal.search.api.model.query;

public final class Sort {
    public final String name;
    public final Order order;

    public Sort(final String name) {
        this(name, Order.ASC);
    }

    public Sort(final String name, final String order) {
        this(name, Order.valueOf(order));
    }

    public Sort(final String name, final Order order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public Order getOrder() {
        return order;
    }

}
