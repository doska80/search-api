package com.vivareal.search.api.controller.v2.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

public final class ResponseStream {

    private OutputStream stream;

    private ResponseStream(OutputStream stream) {
        if (stream == null) throw new IllegalArgumentException("stream cannot be null");
        this.stream = stream;
    }

    public static ResponseStream create(OutputStream stream) {
        return new ResponseStream(stream);
    }

    public <T> void withIterable(Iterable<T> iterable, Function<T, byte[]> fn) throws IOException {
        for (T t : iterable) {
            write(fn.apply(t));
        }
        stream.close();
    }

    public void write(byte[] bytes) throws IOException {
        stream.write(bytes);
        stream.flush();
    }
}
