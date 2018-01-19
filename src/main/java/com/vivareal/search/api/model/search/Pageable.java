package com.vivareal.search.api.model.search;

import static java.lang.String.format;

public interface Pageable extends Indexable {

  int MAX_WINDOW = 10000;

  int getFrom();

  void setFrom(int from);

  int getSize();

  void setSize(int size);

  String getCursorId();

  void setCursorId(String cursorId);

  default void setPaginationValues(final int defaultSize, final int maxSize) {
    if (getFrom() < 0)
      throw new IllegalArgumentException("Parameter [from] must be a positive integer");

    if (getSize() != Integer.MAX_VALUE) {
      if (getSize() < 0 || ((getSize() > maxSize) && maxSizeValidation()))
        throw new IllegalArgumentException(
            format(
                "Parameter [size] must be a positive integer less than %d (default: %d)",
                maxSize, defaultSize));

      setSize(getSize());
    } else {
      setSize(defaultSize);
    }

    if (getFrom() + getSize() > MAX_WINDOW && maxSizeValidation())
      throw new IllegalArgumentException(
          format(
              "Result window is too large, from + size must be less than or equal to: [%d] but was [%d]",
              MAX_WINDOW, getFrom() + getSize()));

    setFrom(getFrom());
  }

  default boolean maxSizeValidation() {
    return false;
  }
}
