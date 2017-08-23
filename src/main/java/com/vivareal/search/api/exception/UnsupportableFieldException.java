package com.vivareal.search.api.exception;

import com.vivareal.search.api.model.query.RelationalOperator;

public class UnsupportableFieldException extends IllegalArgumentException {

    private static final InvalidMessageFunction ERROR_MESSAGE = (invalidField, validField, operator) -> String.format("Field type [ %s ] is not supported for operation [ %s ]. Correct field type is [ %s ]", invalidField, operator.name(), validField);

    public UnsupportableFieldException(final String invalidField, final String validField, RelationalOperator operator) {
        super(ERROR_MESSAGE.apply(invalidField, validField, operator));
    }

    public UnsupportableFieldException(String invalidField, final String validField, RelationalOperator operator, Throwable cause) {
        super(ERROR_MESSAGE.apply(invalidField, validField, operator), cause);
    }

    public UnsupportableFieldException(Throwable cause) {
        super(cause);
    }


}

@FunctionalInterface
interface InvalidMessageFunction {
    String apply(String invalidField, String validField, RelationalOperator operator);
}
