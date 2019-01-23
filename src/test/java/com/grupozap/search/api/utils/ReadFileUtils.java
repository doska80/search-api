package com.grupozap.search.api.utils;

import static java.nio.file.Files.lines;
import static java.nio.file.Paths.get;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URISyntaxException;
import org.slf4j.Logger;

public class ReadFileUtils {

  private static final Logger log = getLogger(ReadFileUtils.class);

  public static String readFileFromResources(final String name) {
    var data = new StringBuilder();

    try {
      var path =
          get(requireNonNull(ReadFileUtils.class.getClassLoader().getResource(name)).toURI());

      var lines = lines(path);
      lines.forEach(line -> data.append(line).append("\n"));
      lines.close();

    } catch (IOException | URISyntaxException e) {
      log.error("Error to read file {} from resources.", name, e);
    }
    return data.toString();
  }
}
