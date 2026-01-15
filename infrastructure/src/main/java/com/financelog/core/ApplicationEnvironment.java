package com.financelog.core;

import software.amazon.awscdk.Tags;
import software.constructs.IConstruct;

/**
 * Represents the logical environment for the application, combining the application name
 * and the deployment stage (e.g., dev, staging, prod).
 * * <p>
 * This class is a central utility used to:
 * </p>
 * <ul>
 * <li>Generate consistent, deterministic names for AWS resources.</li>
 * <li>Apply standard metadata tags to CDK constructs.</li>
 * <li>Ensure naming compatibility with AWS constraints through sanitization.</li>
 * </ul>
 */
public class ApplicationEnvironment {

    private final String applicationName;
    private final DeploymentStage deploymentStage;

    /**
     * Creates a new ApplicationEnvironment.
     * * @param applicationName The name of the application (e.g., "todo-app").
     * @param deploymentStage The target stage (e.g., {@link DeploymentStage#DEV}).
     */
    public ApplicationEnvironment(String applicationName, DeploymentStage deploymentStage) {
        this.applicationName = applicationName;
        this.deploymentStage = deploymentStage;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public DeploymentStage getDeploymentStage(){
        return deploymentStage;
    }

    /**
     * Removes any characters that are typically invalid in AWS resource names.
     * Only alphanumeric characters and hyphens are permitted.
     */
    private String sanitize(String environmentName) {
        return environmentName.replaceAll("[^a-zA-Z0-9-]", "");
    }

    /**
     * Returns a string representation of the environment, typically used as a stack name prefix.
     * Format: {stage}-{applicationName}
     */
    @Override
    public String toString() {
        return sanitize(deploymentStage.getName() + "-" + applicationName);
    }

    /**
     * Prefixes a given string with the environment identifier.
     * * @param string The suffix to append (e.g., "vpc" or "database").
     * @return A combined string: {stage}-{applicationName}-{suffix}
     */
    public String prefix(String string){
        return this + "-" + string;
    }

    /**
     * Prefixes a given string with the environment identifier, ensuring the total length
     * does not exceed AWS service limits (e.g., 32 characters for some resources).
     * * <p>If the length is exceeded, it returns the <b>last</b> N characters of the string.</p>
     * * @param string The suffix to append.
     * @param characterLimit The maximum allowed length.
     * @return A truncated or full prefixed string.
     */
    public String prefix(String string, int characterLimit){
        String name = this + "-" + string;
        if (name.length() <= characterLimit) {
            return name;
        }
        return name.substring(name.length() - characterLimit);
    }

    /**
     * Applies standard environment tags to a CDK construct.
     * * <p>Adds the following tags:</p>
     * <ul>
     * <li><b>deployment</b>: The name of the deployment stage.</li>
     * <li><b>application</b>: The name of the application.</li>
     * </ul>
     * * @param construct The CDK construct (Stack, Resource, etc.) to tag.
     */
    public void tag(IConstruct construct){
        Tags.of(construct).add("deployment", deploymentStage.getName());
        Tags.of(construct).add("application", applicationName);

    }
}

