package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.MockTransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Created by leandropereirapinto on 7/3/17.
 */
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class ElasticsearchQueryAdapterTest {

    private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

    private TransportClient transportClient;

    @Mock
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    @Before
    public void setup() {
        initMocks(this);

        this.queryAdapter = spy(new ElasticsearchQueryAdapter());

        this.transportClient = new MockTransportClient(Settings.EMPTY);

        // initialize variables
        setField(this.queryAdapter, "transportClient", transportClient);
        setField(this.queryAdapter, "settingsAdapter", settingsAdapter);
        setField(this.queryAdapter, "queryDefaultFields", "title.raw,description.raw:2,address.street.raw:5");
        setField(this.queryAdapter, "queryDefaultOperator", "AND");
        setField(this.queryAdapter, "queryDefaultMM", "75%");
        setField(this.queryAdapter, "facetSize", 20);
    }

    @After
    public void closeClient() {
        this.transportClient.close();
    }

    @Test(expected = IndexNotFoundException.class)
    public void shouldBeThrowExceptionWhenIndexIsInvalid() throws Exception {
        doThrow(new IndexNotFoundException("my_index")).when(settingsAdapter).checkIndex(any());
        queryAdapter.getById(any(), anyString());
    }

    @Test
    public void shouldBeBuildGetRequestById() throws Exception {

        String id = "123456";
        String index = "my_index";

        SearchApiRequest request = new SearchApiRequest();
        request.setIndex(index);

        doNothing().when(settingsAdapter).checkIndex(request);

        GetRequestBuilder requestBuilder = queryAdapter.getById(request, id);
        assertEquals(id, requestBuilder.request().id());
        assertEquals(index, requestBuilder.request().index());
        assertEquals(index, requestBuilder.request().type());
    }

    @Test
    @Ignore
    public void query() throws Exception {
    }

}
