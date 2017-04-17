package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.SearchApiResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ListingControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void exampleTest() {
        SearchApiResponse response = this.restTemplate.getForObject("/v2/listings/?q=banos:2", SearchApiResponse.class);

        assertNotNull(response);
        assertEquals(10, ((List)response.getListings()).size());
    }

    @Test
    public void exampleStreamTest() {
        String list = this.restTemplate.execute("/v2/listings/stream?q=banos:2", HttpMethod.GET, null, (ClientHttpResponse client) -> {
            try (Scanner sc = new Scanner(client.getBody())) {
                return sc.nextLine();
            }
        });

        String[] listings = list.split("\\}\\{");
        assertNotNull(list);
        assertEquals(10, listings.length);
    }

}