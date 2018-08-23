package com.grupozap.search.api.controller.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Using NDJSON Format: http://ndjson.org/ */
public final class ResponseStream {

  private static final Logger LOG = LoggerFactory.getLogger(ResponseStream.class);
  private static final char BLANK_LINE = '\n';

  public static <T> void iterate(
      OutputStream stream, Iterator<T[]> iterator, Function<T, byte[]> byteFn) {
    if (stream == null) throw new IllegalArgumentException("stream cannot be null");
    try {
      while (iterator.hasNext()) {
        for (T hit : iterator.next()) {
          stream.write(byteFn.apply(hit));
          stream.write(BLANK_LINE);
        }
        stream.flush();
      }
      stream.write(BLANK_LINE);
      stream.flush();
    } catch (IOException e) {
      LOG.error("write error on iterator stream", e);
    }
  }
}
