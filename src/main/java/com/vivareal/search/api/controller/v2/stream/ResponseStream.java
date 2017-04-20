package com.vivareal.search.api.controller.v2.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ResponseStream {

    private Logger LOG = LoggerFactory.getLogger(ResponseStream.class);

    private OutputStream stream;

    private ResponseStream(OutputStream stream) {
        if (stream == null) throw new IllegalArgumentException("stream cannot be null");
        this.stream = stream;
    }

    public static ResponseStream create(OutputStream stream) {
        return new ResponseStream(stream);
    }

    public <T> void withIterator(Iterator<T[]> iterator, Function<T, byte[]> byteFn) {
        try {
            while (iterator.hasNext()) {
                write(flatArray(iterator.next(), byteFn));
            }
        } catch (IOException e) {
            LOG.error("write error on iterator stream", e);
        }
    }

    <T> byte[] flatArray(T[] array, Function<T, byte[]> byteFn) {
        List<byte[]> bytes = new ArrayList<>(array.length);

        Stream.of(array).map(byteFn).forEach(bytes::add);

        int offset = 0, size = bytes.stream().map(t -> t.length).reduce(0, (a, b) -> a + b);

        byte[] flat = new byte[size];

        for (byte[] by : bytes) {
            System.arraycopy(by, 0, flat, offset, by.length);
            offset += by.length;
        }

        return flat;
    }

    void write(byte[] bytes) throws IOException {
        stream.write(bytes);
        stream.flush();
    }
}
