package com.vivareal.search.api.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SearchApiRequestTest {

    @Test
    public void shouldValidateQueryString() {
        SearchApiRequest request = new SearchApiRequest();
        System.out.println(request);
    }

}
