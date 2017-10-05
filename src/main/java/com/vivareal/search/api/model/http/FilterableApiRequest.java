package com.vivareal.search.api.model.http;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.vivareal.search.api.model.search.*;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

public class FilterableApiRequest extends BaseApiRequest implements Filterable, Queryable, Pageable, Sortable {

    @ApiModelProperty(value = "Query string")
    private String q;

    @ApiModelProperty(value = "Minimum should match (-100..+100)", example = "10, 75%")
    private String mm;

    @ApiModelProperty(value = "Query DSL", example = "field1:3 AND field2:2 AND(field3=1 OR (field4 IN [1,\"abc\"] AND field5 <> 3))")
    private String filter;

    @ApiModelProperty(value = "Field list that will be filtered for query string", example = "field1, field2, field3")
    private Set<String> fields;

    @ApiModelProperty(value = "Sorting in the format: field (ASC|DESC), default sort order is ascending, multiple sort are supported", example = "field1 ASC, field2 DESC")
    private Set<String> sort;

    @ApiModelProperty("From index to start the search from")
    private int from = 0;

    @ApiModelProperty("The number of search hits to return")
    private int size = Integer.MAX_VALUE;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getMm() {
        return mm;
    }

    public void setMm(String mm) {
        this.mm = mm;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public Set<String> getSort() {
        return sort;
    }

    public void setSort(Set<String> sort) {
        this.sort = sort;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    protected ToStringHelper addValuesToStringHelper(ToStringHelper stringHelper) {
        return super.addValuesToStringHelper(stringHelper)
        .add("filter", getFilter())
        .add("q", getQ())
        .add("mm", getMm())
        .add("fields", getFields())
        .add("from", getFrom())
        .add("size", getSize())
        .add("sort", getSort());
    }
}
