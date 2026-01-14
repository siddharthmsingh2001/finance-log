package com.financelog.core;

import software.amazon.awscdk.Tags;
import software.constructs.IConstruct;

public class ApplicationEnvironment {

    private final String applicationName;
    private final DeploymentStage deploymentStage;

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

    private String sanitize(String environmentName) {
        return environmentName.replaceAll("[^a-zA-Z0-9-]", "");
    }

    @Override
    public String toString() {
        return sanitize(deploymentStage.getName() + "-" + applicationName);
    }

    public String prefix(String string){
        return this + "-" + string;
    }

    public String prefix(String string, int characterLimit){
        String name = this + "-" + string;
        if (name.length() <= characterLimit) {
            return name;
        }
        return name.substring(name.length() - characterLimit);
    }

    public void tag(IConstruct construct){
        Tags.of(construct).add("deployment", deploymentStage.getName());
        Tags.of(construct).add("application", applicationName);

    }
}

