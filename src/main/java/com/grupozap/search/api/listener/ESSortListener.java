package com.grupozap.search.api.listener;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT;
import static java.lang.String.valueOf;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.listener.SearchSort;
import com.grupozap.search.api.model.listener.SearchSort.SearchSortBuilder;
import com.grupozap.search.api.model.listener.rfq.Rfq;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("esSortListener")
public class ESSortListener implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(ESSortListener.class);

  private final Map<String, SearchSort> searchSortMap;

  public ESSortListener() {
    this.searchSortMap = new ConcurrentHashMap<>();
  }

  public SearchSort getSearchSort(String index) {
    return this.searchSortMap.get(index);
  }

  public Optional<Rfq> getRfq(FilterableApiRequest request) {
    var disableRfq =
        request.isDisableRfq() != null
            ? request.isDisableRfq()
            : this.searchSortMap.get(request.getIndex()).isDisableRfq();

    if (!disableRfq
        && this.searchSortMap.containsKey(request.getIndex())
        && getSortField(request) != null) {
      var field = getSortField(request);
      return hasRfqConfigured(request.getIndex(), field)
          ? of(this.searchSortMap.get(request.getIndex()).getSorts().get(field).getRfq())
          : empty();
    }
    return empty();
  }

  private boolean hasRfqConfigured(String index, String field) {
    return !isEmpty(this.searchSortMap.get(index).getSorts())
        && this.searchSortMap.get(index).getSorts().containsKey(field)
        && this.searchSortMap.get(index).getSorts().get(field).getRfq() != null;
  }

  private String getSortField(FilterableApiRequest request) {
    return !isBlank(request.getSort())
        ? request.getSort().split("\\s")[0]
        : this.searchSortMap.get(request.getIndex()).getDefaultSort();
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    Map<String, Object> esSort = ES_SORT.getValue(event.getIndex());
    if (nonNull(esSort)) {
      searchSortMap.put(
          event.getIndex(),
          new SearchSortBuilder()
              .disabled((boolean) esSort.getOrDefault("disabled", true))
              .disableRfq((boolean) esSort.getOrDefault("disable_rfq", true))
              .defaultSort(valueOf(esSort.get("default_sort")))
              .sorts(esSort)
              .build());

      LOG.info("Refreshing es.sort. {}", this.searchSortMap.toString());
    } else {
      this.searchSortMap.remove(event.getIndex());
    }
  }
}
