package com.financelog.core;

import java.util.Arrays;

/**
 * Defines the supported deployment environments for the infrastructure.
 */
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

    /**
     * Parses a string value into a DeploymentStage.
     * * @param value The raw string (e.g., from CDK context).
     * @return The matching DeploymentStage.
     * @throws IllegalArgumentException if the value doesn't match any stage.
     */
    public static DeploymentStage from(String value) {
        return Arrays.stream(values())
                .filter(stage -> stage.name.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown deployment stage: " + value)
                );
    }
}
