package com.grupozap.search.api.model.listener;

import com.grupozap.search.api.model.listener.rescore.SortRescore;
import com.grupozap.search.api.model.listener.rfq.Rfq;
import com.grupozap.search.api.model.listener.script.ScriptField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      var sorts = new HashMap<String, MultiSort>();
      ((List<Map<String, Map<String, Map<String, Object>>>>) esSort.get("sorts"))
          .forEach(
              sort ->
                  sort.forEach(
                      (key, entryValue) -> {
                        var multiSort =
                            new MultiSort.MultiSortBuilder()
                                .rfq(Rfq.build(entryValue))
                                .rescores(SortRescore.build(entryValue))
                                .scripts(ScriptField.build(entryValue))
                                .build();
                        sorts.put(key, multiSort);
                      }));

      this.sorts = sorts;
      return this;
    }

    public SearchSort build() {
      return new SearchSort(this);
    }
  }
}
