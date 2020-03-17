package com.grupozap.search.api.listener;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.utils.EsSortUtils.DEFAULT_SORT;
import static com.grupozap.search.api.utils.EsSortUtils.DISABLED_SORT;
import static com.grupozap.search.api.utils.EsSortUtils.SortType.LTR_RESCORE_TYPE;
import static com.grupozap.search.api.utils.EsSortUtils.SortType.RQF_TYPE;
import static java.util.Map.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.utils.EsSortUtils;
import java.util.List;
import org.junit.Test;

public class ESSortListenerTest {

  private ESSortListener esSortListener = new ESSortListener();

  @Test
  public void shouldCreateSearchSort() {
    var event = new RemotePropertiesUpdatedEvent("", INDEX_NAME);
    ES_SORT.setValue(
        event.getIndex(),
        new EsSortUtils()
            .buildEsSort(
                "rescore_default",
                of(
                    "rescores",
                    List.of(LTR_RESCORE_TYPE.getSortType()),
                    "rfq",
                    RQF_TYPE.getSortType())));
    esSortListener.onApplicationEvent(event);

    var searchSort = esSortListener.getSearchSort(INDEX_NAME);

    assertFalse(searchSort.getSorts().isEmpty());
    assertEquals(DISABLED_SORT, searchSort.isDisabled());
    assertEquals(DEFAULT_SORT, searchSort.getDefaultSort());

    // validating RFQ
    assertEquals("field1", searchSort.getSorts().get(DEFAULT_SORT).getRfq().getField());
    assertEquals("log", searchSort.getSorts().get(DEFAULT_SORT).getRfq().getFunction());
    assertEquals(4.0, searchSort.getSorts().get(DEFAULT_SORT).getRfq().getScalingFactor(), 0);
    assertEquals(1.0, searchSort.getSorts().get(DEFAULT_SORT).getRfq().getBoost(), 0);
    assertEquals(0.0, searchSort.getSorts().get(DEFAULT_SORT).getRfq().getExponent(), 0);
    assertEquals(0.0, searchSort.getSorts().get(DEFAULT_SORT).getRfq().getPivot(), 0);
  }
}
