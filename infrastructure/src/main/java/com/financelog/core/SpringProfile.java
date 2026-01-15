package com.financelog.core;

import java.util.Arrays;

/**
 * Defines the Spring Boot profiles supported by the application.
 * * <p>
 * This enum maps the infrastructure's deployment stage to the corresponding
 * Spring {@code @Profile} within the application code. It ensures that
 * environment-specific configurations (like database credentials, logging levels,
 * or feature flags) are correctly applied at runtime.
 * </p>
 */
public enum SpringProfile {

    /** * Profile for local development or sandbox environments.
     */
    DEV("dev"),
    /** * Profile for the staging/pre-production environment, used for integration testing.
     */
    STAGING("staging"),

    PROD("prod");
    /** * Profile for the production environment with strict security and performance settings.
     */
    private final String name;

    /**
     * @param name The string representation of the profile as used in
     * {@code spring.profiles.active}.
     */
    SpringProfile(String name){
        this.name = name;
    }

    /**
     * Returns the string representation of the Spring profile.
     * @return the profile name (e.g., "prod")
     */
    public String getName() {
        return name;
    }

    /**
     * Factory method to resolve a {@code SpringProfile} from a string value,
     * typically sourced from CDK context or environment variables.
     * * <p>The search is case-insensitive.</p>
     * * @param value The string value to parse.
     * @return The matching {@link SpringProfile} constant.
     * @throws IllegalArgumentException if the value does not match any known profile.
     */
    public static SpringProfile from(String value) {
        return Arrays.stream(values())
                .filter(stage -> stage.name.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown spring profile: " + value)
                );
    }
}
