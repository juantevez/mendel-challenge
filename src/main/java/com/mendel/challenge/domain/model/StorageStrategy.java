package com.mendel.challenge.domain.model;

public enum StorageStrategy {
    IN_MEMORY("memory"),
    REDIS("redis");

    private final String value;

    StorageStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StorageStrategy fromString(String value) {
        for (StorageStrategy strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown storage strategy: " + value);
    }
}
