package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

import java.util.*;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public final class Sort extends AbstractSet<Sort.Item> {

    private Set<Item> items = new LinkedHashSet<>();

    public Sort(Field field, OrderOperator orderOperator) {
        items.add(new Item(field, orderOperator));
    }

    public Sort(List<Sort> sortList) {
        sortList.stream().flatMap(s -> s.items.stream()).forEach(items::add);
    }

    @Override
    public Iterator<Item> iterator() {
        return items.iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        return items.stream().map(Item::toString).collect(joining(" "));
    }

    public static class Item {
        private final Field field;
        private final OrderOperator orderOperator;

        private Item(Field field, OrderOperator orderOperator) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return equal(field, item.field) && equal(orderOperator, item.orderOperator);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(field, orderOperator);
        }
    }
}
