package com.grupozap.search.api.model.http;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;

import java.util.Set;

public class SearchApiRequestBuilder {

  public static final String INDEX_NAME = "index-name";
  public static final String INDEX_ALIAS_NAME = "index-name-alias";

  private SearchApiRequestBuilder() {}

  public static BasicRequestBuilder basic() {
    return BasicRequestBuilder.create();
  }

  public static FilterableRequestBuilder filterable() {
    return FilterableRequestBuilder.create();
  }

  public static ComplexRequestBuilder create() {
    return ComplexRequestBuilder.create();
  }

  public static class BasicRequestBuilder {
    protected String index;
    protected Set<String> includeFields;
    protected Set<String> excludeFields;

    private BasicRequestBuilder() {}

    public static BasicRequestBuilder create() {
      return new BasicRequestBuilder();
    }

    public BasicRequestBuilder index(final String index) {
      this.index = index;
      return this;
    }

    public BasicRequestBuilder includeFields(Set<String> includeFields) {
      this.includeFields = includeFields;
      return this;
    }

    public BasicRequestBuilder excludeFields(Set<String> excludeFields) {
      this.excludeFields = excludeFields;
      return this;
    }

    public BaseApiRequest build() {
      var request = new BaseApiRequest();

      if (allNotNull(index)) request.setIndex(index);

      if (allNotNull(includeFields)) request.setIncludeFields(includeFields);

      if (allNotNull(excludeFields)) request.setExcludeFields(excludeFields);

      return request;
    }
  }

  public static class FilterableRequestBuilder extends BasicRequestBuilder {
    protected String mm;
    protected String factorField;
    protected String factorModifier;
    protected Set<String> fields;

    protected String filter;
    protected String sort;
    protected String q;

    private FilterableRequestBuilder() {}

    public static FilterableRequestBuilder create() {
      return new FilterableRequestBuilder();
    }

    @Override
    public FilterableRequestBuilder index(final String index) {
      this.index = index;
      return this;
    }

    @Override
    public FilterableRequestBuilder includeFields(Set<String> includeFields) {
      this.includeFields = includeFields;
      return this;
    }

    @Override
    public FilterableRequestBuilder excludeFields(Set<String> excludeFields) {
      this.excludeFields = excludeFields;
      return this;
    }

    public FilterableRequestBuilder q(String q) {
      this.q = q;
      return this;
    }

    public FilterableRequestBuilder mm(String mm) {
      this.mm = mm;
      return this;
    }

    public FilterableRequestBuilder factorField(String factorField) {
      this.factorField = factorField;
      return this;
    }

    public FilterableRequestBuilder factorModifier(String factorModifier) {
      this.factorModifier = factorModifier;
      return this;
    }

    public FilterableRequestBuilder fields(Set<String> fields) {
      this.fields = fields;
      return this;
    }

    public FilterableRequestBuilder filter(String filter) {
      this.filter = filter;
      return this;
    }

    public FilterableRequestBuilder sort(String sort) {
      this.sort = sort;
      return this;
    }

    @Override
    public FilterableApiRequest build() {
      var request = new FilterableApiRequest();

      if (allNotNull(index)) request.setIndex(index);

      if (allNotNull(includeFields)) request.setIncludeFields(includeFields);

      if (allNotNull(excludeFields)) request.setExcludeFields(excludeFields);

      if (allNotNull(q)) request.setQ(q);

      if (allNotNull(mm)) request.setMm(mm);

      if (allNotNull(factorField)) request.setFactorField(factorField);

      if (allNotNull(factorModifier)) request.setFactorModifier(factorModifier);

      if (allNotNull(fields)) request.setFields(fields);

      if (allNotNull(filter)) request.setFilter(filter);

      if (allNotNull(sort)) request.setSort(sort);

      return request;
    }
  }

  public static class ComplexRequestBuilder extends FilterableRequestBuilder {

    private int from;
    private int size;
    private int facetSize = MAX_VALUE;
    private Set<String> facets;

    private ComplexRequestBuilder() {}

    public static ComplexRequestBuilder create() {
      return new ComplexRequestBuilder();
    }

    @Override
    public ComplexRequestBuilder index(final String index) {
      this.index = index;
      return this;
    }

    @Override
    public ComplexRequestBuilder includeFields(Set<String> includeFields) {
      this.includeFields = includeFields;
      return this;
    }

    @Override
    public ComplexRequestBuilder excludeFields(Set<String> excludeFields) {
      this.excludeFields = excludeFields;
      return this;
    }

    @Override
    public ComplexRequestBuilder q(String q) {
      this.q = q;
      return this;
    }

    @Override
    public ComplexRequestBuilder mm(String mm) {
      this.mm = mm;
      return this;
    }

    @Override
    public ComplexRequestBuilder factorField(String factorField) {
      this.factorField = factorField;
      return this;
    }

    @Override
    public ComplexRequestBuilder factorModifier(String factorModifier) {
      this.factorModifier = factorModifier;
      return this;
    }

    @Override
    public ComplexRequestBuilder fields(Set<String> fields) {
      this.fields = fields;
      return this;
    }

    @Override
    public ComplexRequestBuilder filter(String filter) {
      this.filter = filter;
      return this;
    }

    @Override
    public ComplexRequestBuilder sort(String sort) {
      this.sort = sort;
      return this;
    }

    public ComplexRequestBuilder facets(Set<String> facets) {
      this.facets = facets;
      return this;
    }

    public ComplexRequestBuilder facetSize(int facetSize) {
      this.facetSize = facetSize;
      return this;
    }

    public ComplexRequestBuilder from(int from) {
      this.from = from;
      return this;
    }

    public ComplexRequestBuilder size(int size) {
      this.size = size;
      return this;
    }

    @Override
    public SearchApiRequest build() {
      var request = new SearchApiRequest();

      if (allNotNull(index)) request.setIndex(index);

      if (allNotNull(includeFields)) request.setIncludeFields(includeFields);

      if (allNotNull(excludeFields)) request.setExcludeFields(excludeFields);

      if (allNotNull(q)) request.setQ(q);

      if (allNotNull(mm)) request.setMm(mm);

      if (allNotNull(factorField)) request.setFactorField(factorField);

      if (allNotNull(factorModifier)) request.setFactorModifier(factorModifier);

      if (allNotNull(fields)) request.setFields(fields);

      if (allNotNull(filter)) request.setFilter(filter);

      if (allNotNull(sort)) request.setSort(sort);

      if (allNotNull(facets)) request.setFacets(facets);

      if (allNotNull(facetSize)) request.setFacetSize(facetSize);

      if (allNotNull(from)) request.setFrom(from);

      if (allNotNull(size)) request.setSize(size);

      return request;
    }
  }
}
