package com.grupozap.search.api.model;

import static java.lang.Integer.MAX_VALUE;
import static org.junit.Assert.assertEquals;

import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.search.Pageable;
import org.junit.Test;

public class SearchApiRequestTest {

  private static final int DEFAULT_SIZE_FROM = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 40;

  @Test
  public void shouldValidateQueryString() {
    var request = new SearchApiRequest();
    System.out.println(request);
  }

  @Test
  public void shouldKeepValueWhenSizeFromIsOk() {
    var request = new SearchApiRequest();
    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);

    var from = 0;
    request.setFrom(from);
    assertEquals(from, request.getFrom());

    from = 10;
    request.setFrom(from);
    assertEquals(from, request.getFrom());

    from = MAX_VALUE;
    request.setFrom(from);
    assertEquals(from, request.getFrom());
  }

  @Test
  public void shouldApplyDefaultSizeFromPaginationIfSizeFromNotBeInformed() {
    var request = new SearchApiRequest();
    assertEquals(0, request.getFrom());

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
    assertEquals(DEFAULT_SIZE_FROM, request.getFrom());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldApplyDefaultSizeFromPaginationIfSizeFromIsNegative() {
    var request = new SearchApiRequest();
    request.setFrom(-10);

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
  }

  @Test
  public void shouldKeepValueWhenSizeIsOk() {
    var request = new SearchApiRequest();
    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);

    var size = 0;
    request.setSize(size);
    assertEquals(size, request.getSize());

    size = 10;
    request.setSize(size);
    assertEquals(size, request.getSize());

    size = 40;
    request.setSize(size);
    assertEquals(size, request.getSize());
  }

  @Test
  public void shouldApplyDefaultSizePaginationIfSizeNotBeInformed() {
    var request = new SearchApiRequest();
    assertEquals(Integer.MAX_VALUE, request.getSize());

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
    assertEquals(DEFAULT_SIZE, request.getSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldApplyDefaultSizePaginationIfSizeIsNegative() {
    var request = new SearchApiRequest();
    request.setSize(-10);

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldApplyDefaultSizePaginationIfSizeIsGreaterThanMaxSize() {
    var request = new SearchApiRequest();
    request.setSize(41);

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldApplyDefaultSizePaginationIfSizeIsGreaterThanMaxWindow() {
    var request = new SearchApiRequest();
    request.setSize(Pageable.MAX_WINDOW + 1);

    request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
  }
}
