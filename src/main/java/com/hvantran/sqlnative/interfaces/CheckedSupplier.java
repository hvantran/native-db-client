package com.hvantran.sqlnative.interfaces;

import java.util.function.Supplier;

@FunctionalInterface
public interface CheckedSupplier<T> extends Supplier<T> {

    default T get() {
        try {
            return getThrows();
        } catch (Exception exception) {
            throw new AppException(exception);
        }
    }

    T getThrows() throws Exception;
}
