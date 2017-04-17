package com.vivareal.search.api.controller.v2.stream;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResponseStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenStreamIsNull() {
        ResponseStream.create(null);
    }

    @Test
    public void shouldCallWriteAndFlush() throws IOException {
        OutputStream mockStream = mock(OutputStream.class);

        ResponseStream.create(mockStream).write(new byte[0]);

        verify(mockStream).write(any(byte[].class));
        verify(mockStream).flush();
    }

    @Test
    public void shouldCreateStreamWithIterable() throws IOException {
        OutputStream mockStream = mock(OutputStream.class);

        ResponseStream.create(mockStream)
                .withIterable(Collections::emptyIterator, Function.identity());

        verify(mockStream).close();
    }
}
