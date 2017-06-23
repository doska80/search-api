package com.vivareal.search.api.controller;

import com.vivareal.search.api.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("prod")
@WebMvcTest(SearchController.class)
public class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SearchService searchService;

    @Test
    public void dummyTest() {
        assert 1==1;
    }

//    @Test
//    public void exampleTest() {
//        SearchApiResponse response = this.restTemplate.getForObject("/v2/listings/?q=banos:2", SearchApiResponse.class);
//
//        assertNotNull(response);
//        assertEquals(10, ((List)response.getResult()).size());
//    }
//

    @Test
    public void exampleStreamTest() throws Exception {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";

        mvc.perform(get("/v2/listings/stream"))
                .andDo(handler -> handler.getResponse().getWriter().write(json))
                .andExpect(status().isOk())
                //.andExpect(content().contentTypeCompatibleWith("application/x-ndjson"))
                .andExpect(request().asyncStarted())
                .andExpect(content().string(json));
    }

}
