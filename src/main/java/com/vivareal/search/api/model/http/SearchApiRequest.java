package com.vivareal.search.api.model.http;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.vivareal.search.api.model.search.Facetable;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;

public class SearchApiRequest extends FilterableApiRequest implements Facetable {

  @ApiModelProperty(value = "Facet field list", example = "field, field2, field3")
  private Set<String> facets;

  @ApiModelProperty("Sets the size of facets")
  private int facetSize = Integer.MAX_VALUE;

  public Set<String> getFacets() {
    return facets;
  }

  public void setFacets(Set<String> facets) {
    this.facets = facets;
  }

  public int getFacetSize() {
    return facetSize;
  }

  public void setFacetSize(int facetSize) {
    this.facetSize = facetSize;
  }

  protected ToStringHelper addValuesToStringHelper(ToStringHelper stringHelper) {
    return super.addValuesToStringHelper(stringHelper)
        .add("facets", getFacets())
        .add("facetSize", getFacetSize());
  }

  @Override
  public boolean maxSizeValidation() {
    return true;
  }
}
