package com.hvantran.sqlnative.utils;

import com.hvantran.sqlnative.interfaces.CheckedSupplier;

public class InstanceUtils {

    private InstanceUtils() {

    }

    public static Object newInstance(Class<?> klass) {
        CheckedSupplier<Object> instanceSup = () -> klass.getDeclaredConstructor().newInstance();
        return instanceSup.get();
    }
}
