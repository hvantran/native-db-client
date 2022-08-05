package com.hvantran.sqlnative.utils;

import java.util.AbstractMap;

public class Pair<K, V> extends AbstractMap.SimpleEntry<K, V> {

    private Pair(K key, V value) {
        super(key, value);
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }
}
