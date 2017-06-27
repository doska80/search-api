package com.vivareal.search.api.model.query;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public final class Sort extends AbstractList<Sort.Item> {
    private List<Item> items = new ArrayList<>();

    public Sort(Field field, OrderOperator orderOperator) {
        items.add(new Item(field, orderOperator));
    }

    public Sort(List<Sort> sortList) {
        sortList.stream().flatMap(s -> s.items.stream()).forEach(items::add);
    }

    @Override
    public Item get(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        return items.stream().map(Item::toString).collect(Collectors.joining(" "));
    }

    public static class Item {
        private final Field field;
        private final OrderOperator orderOperator;

        public Item(Field field, OrderOperator orderOperator) {
            this.field = field;
            this.orderOperator = orderOperator;
        }

        public Field getField() {
            return field;
        }

        public OrderOperator getOrderOperator() {
            return orderOperator;
        }

        @Override
        public String toString() {
            return format("%s %s", field, orderOperator);
        }
    }
}
