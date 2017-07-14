package com.vivareal.search.api.exception;

import java.util.function.Function;

/**
 * Created by leandropereirapinto on 7/12/17.
 */
public class PropertyNotFoundException extends IllegalArgumentException {

    private static final Function<String, String> ERROR_MESSAGE = property -> String.format("Property %s not found", property);

    public PropertyNotFoundException(final String property) {
        super(ERROR_MESSAGE.apply(property));
    }

    public PropertyNotFoundException(String property, Throwable cause) {
        super(ERROR_MESSAGE.apply(property), cause);
    }

    public PropertyNotFoundException(Throwable cause) {
        super(cause);
    }
}
