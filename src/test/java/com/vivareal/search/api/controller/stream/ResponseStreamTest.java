package com.vivareal.search.api.controller.stream;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ResponseStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenStreamIsNull() {
        ResponseStream.iterate(null, null, null);
    }

    @Test
    public void shouldSuccessfulIterate() throws IOException {
        OutputStream mockStream = mock(OutputStream.class);

        final String[][] data = {
            {"tincas", "urubu", "pizza"},
            {"socks", "pinto"},
            {}
        };

        class MyIterator implements Iterator<String[]> {

            int counter = 0;

            @Override
            public boolean hasNext() {
                return counter < data.length;
            }

            @Override
            public String[] next() {
                return data[counter++];
            }
        }

        ResponseStream.iterate(mockStream, new MyIterator(), String::getBytes);

        int callCount = 0;
        for (String[] hits : data) {
            for (String hit: hits) {
                verify(mockStream, times(1)).write(hit.getBytes());
                ++callCount;
            }
        }

        verify(mockStream, times(callCount)).write('\n');
        verify(mockStream, times(data.length)).flush();
    }
}
