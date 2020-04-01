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
import com.grupozap.search.api.query.LtrQueryBuilder;
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
    var rqfSort = searchSort.getSorts().get(DEFAULT_SORT).getRfq();
    assertEquals("field1", rqfSort.getField());
    assertEquals("log", rqfSort.getFunction());
    assertEquals(4.0, rqfSort.getScalingFactor(), 0);
    assertEquals(1.0, rqfSort.getBoost(), 0);
    assertEquals(0.0, rqfSort.getExponent(), 0);
    assertEquals(0.0, rqfSort.getPivot(), 0);

    // validating LTR_RESCORE
    var rescores = searchSort.getSorts().get(DEFAULT_SORT).getRescores();
    assertEquals(1, rescores.size());
    assertEquals(500.0, rescores.get(0).getWindowSize(), 0);
    assertEquals(1.0, rescores.get(0).getQueryWeight(), 0);
    assertEquals(1.0, rescores.get(0).getQueryWeight(), 0);
    assertEquals(LtrQueryBuilder.class, rescores.get(0).getQueryBuilder().getClass());
  }
}
