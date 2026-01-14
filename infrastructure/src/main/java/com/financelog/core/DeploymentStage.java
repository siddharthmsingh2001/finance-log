package com.financelog.core;

import java.util.Arrays;

public enum DeploymentStage {

    DEV("dev"),
    STAGING("staging"),
    PROD("prod");

    private final String name;

    DeploymentStage(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DeploymentStage from(String value) {
        return Arrays.stream(values())
                .filter(stage -> stage.name.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown deployment stage: " + value)
                );
    }
}
