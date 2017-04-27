package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.service.ListingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebMvcTest(ListingController.class)
public class ListingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ListingService listingService;

    @Test
    public void dummyTest() {
        assert 1==1;
    }

//    @Test
//    public void exampleTest() {
//        SearchApiResponse response = this.restTemplate.getForObject("/v2/listings/?q=banos:2", SearchApiResponse.class);
//
//        assertNotNull(response);
//        assertEquals(10, ((List)response.getListings()).size());
//    }
//

    @Test
    public void exampleStreamTest() throws Exception {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";

        mvc.perform(get("/v2/listings/stream-spring"))
                .andDo(handler -> handler.getResponse().getWriter().write(json))
                .andExpect(status().isOk())
                //.andExpect(content().contentTypeCompatibleWith("application/x-ndjson"))
                .andExpect(request().asyncStarted())
                .andExpect(content().string(json));
    }

}