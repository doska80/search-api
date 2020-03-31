package com.grupozap.search.api.model.listener;

import static java.util.stream.Collectors.toMap;

import com.grupozap.search.api.model.listener.MultiSort.MultiSortBuilder;
import com.grupozap.search.api.model.listener.rescore.SortRescore;
import com.grupozap.search.api.model.listener.rfq.Rfq;
import com.grupozap.search.api.model.listener.script.ScriptField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SearchSort {

  private final boolean disabled;
  private final boolean disableRfq;
  private final String defaultSort;
  private final Map<String, MultiSort> sorts;

  private SearchSort(SearchSortBuilder searchSortBuilder) {
    this.disabled = searchSortBuilder.disabled;
    this.disableRfq = searchSortBuilder.disableRfq;
    this.defaultSort = searchSortBuilder.defaultSort;
    this.sorts = searchSortBuilder.sorts;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public boolean isDisableRfq() {
    return disableRfq;
  }

  public String getDefaultSort() {
    return defaultSort;
  }

  public Map<String, MultiSort> getSorts() {
    return sorts;
  }

  public static class SearchSortBuilder {

    private boolean disabled;
    private boolean disableRfq;
    private String defaultSort;
    private Map<String, MultiSort> sorts = new HashMap<>();

    public SearchSortBuilder disabled(boolean disabled) {
      this.disabled = disabled;
      return this;
    }

    public SearchSortBuilder disableRfq(boolean disableRfq) {
      this.disableRfq = disableRfq;
      return this;
    }

    public SearchSortBuilder defaultSort(String defaultSort) {
      this.defaultSort = defaultSort;
      return this;
    }

    @SuppressWarnings("unchecked")
    public SearchSortBuilder sorts(Map<String, Object> esSort) {
      this.sorts =
          ((List<Map<String, Map<String, Map<String, Object>>>>) esSort.get("sorts"))
              .stream()
                  .flatMap(v -> v.entrySet().stream())
                  .collect(
                      toMap(
                          Entry::getKey,
                          entry ->
                              new MultiSortBuilder()
                                  .rfq(Rfq.build(entry.getValue()))
                                  .rescores(SortRescore.build(entry.getValue()))
                                  .scripts(ScriptField.build(entry.getValue()))
                                  .build()));

      return this;
    }

    public SearchSort build() {
      return new SearchSort(this);
    }
  }
}
