package com.vivareal.search.api.model;

import com.vivareal.search.api.model.http.SearchApiRequest;
import org.junit.Test;

import static java.lang.Integer.MAX_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SearchApiRequestTest {

    private static final int DEFAULT_SIZE_FROM = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 40;

    @Test
    public void shouldValidateQueryString() {
        SearchApiRequest request = new SearchApiRequest();
        System.out.println(request);
    }

    @Test
    public void shouldKeepValueWhenSizeFromIsOk() {
        SearchApiRequest request = new SearchApiRequest();
        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);

        int from = 0;
        request.setFrom(from);
        assertEquals(from, request.getFrom().intValue());

        from = 10;
        request.setFrom(from);
        assertEquals(from, request.getFrom().intValue());

        from = MAX_VALUE;
        request.setFrom(from);
        assertEquals(from, request.getFrom().intValue());
    }

    @Test
    public void shouldApplyDefaultSizeFromPaginationIfSizeFromNotBeInformed() {
        SearchApiRequest request = new SearchApiRequest();
        assertNull(request.getFrom());

        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
        assertEquals(DEFAULT_SIZE_FROM, request.getFrom().intValue());
    }

    @Test
    public void shouldApplyDefaultSizeFromPaginationIfSizeFromIsNegative() {
        SearchApiRequest request = new SearchApiRequest();
        request.setFrom(-10);

        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
        assertEquals(DEFAULT_SIZE_FROM, request.getFrom().intValue());
    }

    @Test
    public void shouldKeepValueWhenSizeIsOk() {
        SearchApiRequest request = new SearchApiRequest();
        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);

        int size = 0;
        request.setSize(size);
        assertEquals(size, request.getSize().intValue());

        size = 10;
        request.setSize(size);
        assertEquals(size, request.getSize().intValue());

        size = 40;
        request.setSize(size);
        assertEquals(size, request.getSize().intValue());
    }

    @Test
    public void shouldApplyDefaultSizePaginationIfSizeNotBeInformed() {
        SearchApiRequest request = new SearchApiRequest();
        assertNull(request.getSize());

        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
        assertEquals(DEFAULT_SIZE, request.getSize().intValue());
    }

    @Test
    public void shouldApplyDefaultSizePaginationIfSizeIsNegative() {
        SearchApiRequest request = new SearchApiRequest();
        request.setSize(-10);

        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
        assertEquals(DEFAULT_SIZE, request.getSize().intValue());
    }

    @Test
    public void shouldApplyDefaultSizePaginationIfSizeIsGreaterThanMaxSize() {
        SearchApiRequest request = new SearchApiRequest();
        request.setSize(41);

        request.setPaginationValues(DEFAULT_SIZE, MAX_SIZE);
        assertEquals(DEFAULT_SIZE, request.getSize().intValue());
    }

}
