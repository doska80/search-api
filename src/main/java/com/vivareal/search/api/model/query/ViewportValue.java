package com.vivareal.search.api.model.query;

import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.apache.lucene.geo.GeoUtils.checkLatitude;
import static org.apache.lucene.geo.GeoUtils.checkLongitude;

public class ViewportValue extends Value {
    public ViewportValue(List<List<Value>> content) {
        super(content);

        if (CollectionUtils.isEmpty(content))
            throw new IllegalArgumentException("The viewport cannot be empty");

        if (content.size() != 2)
            throw new IllegalArgumentException("The viewport does have lat/long pair");

        if (content.get(0) == null || content.get(1) == null)
            throw new IllegalArgumentException("The viewport cannot be null");

        if (content.get(0).size() != 2 || content.get(1).size() != 2)
            throw new IllegalArgumentException("The viewport pair must have a lat/long");

        content.forEach(point -> {
            checkLatitude(point.get(0).value());
            checkLongitude(point.get(1).value());
        });
    }
}
