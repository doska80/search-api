package com.grupozap.search.api.itest.configuration.data;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StandardDatasetAsserts {

  private static final Predicate<Integer> ALWAYS_TRUE = returnAll -> true;

  private final int standardDatasetSize;
  private final int defaultPageSize;

  @Autowired
  public StandardDatasetAsserts(
      @Value("${itest.standard.dataset.size}") Integer standardDatasetSize,
      @Value("${es.default.size}") Integer defaultPageSize) {
    this.standardDatasetSize = standardDatasetSize;
    this.defaultPageSize = defaultPageSize;
  }

  public List<Integer> allIdsUntil(int id) {
    var intStream = rangeClosed(1, id);
    return getIds(intStream);
  }

  public Set<Integer> idsBetween(int from, int to) {
    var intStream = rangeClosed(from, to);
    return new LinkedHashSet<>(getIds(intStream));
  }

  public <T> List<T> allIdsUntil(int id, Function<Integer, T> function) {
    return getIds(rangeClosed(1, id), ALWAYS_TRUE, standardDatasetSize, function);
  }

  public Set<Integer> even() {
    return new LinkedHashSet<>(getIds(rangeClosed(1, standardDatasetSize), TestData::isEven));
  }

  public Set<Integer> odd() {
    return new LinkedHashSet<>(getIds(rangeClosed(1, standardDatasetSize), TestData::isOdd));
  }

  private List<Integer> getIds(IntStream intStream) {
    return getIds(intStream, ALWAYS_TRUE);
  }

  private List<Integer> getIds(IntStream intStream, Predicate<Integer> filter) {
    return getIds(intStream, filter, standardDatasetSize);
  }

  private List<Integer> getIds(IntStream intStream, Predicate<Integer> filter, int limit) {
    return getIds(intStream, filter, limit, identity());
  }

  private <T> List<T> getIds(
      IntStream intStream, Predicate<Integer> filter, int limit, Function<Integer, T> function) {
    return intStream.boxed().filter(filter).limit(limit).map(function).collect(toList());
  }
}
