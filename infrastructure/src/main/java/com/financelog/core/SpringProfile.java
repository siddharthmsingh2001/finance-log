package com.financelog.core;

import java.util.Arrays;

public enum SpringProfile {

    DEV("dev"),
    STAGING("staging"),
    PROD("prod");

    private final String name;

    SpringProfile(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SpringProfile from(String value) {
        return Arrays.stream(values())
                .filter(stage -> stage.name.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown spring profile: " + value)
                );
    }
}
