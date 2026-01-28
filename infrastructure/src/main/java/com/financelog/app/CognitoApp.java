package com.financelog.app;


import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.platform.cognito.CognitoStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

public class CognitoApp {

    public static void main(String[] args) {

        App app = new App();

        // Resolve the logical deployment stage (e.g. dev, staging, prod)
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        // Application name forms the base for many naming conventions
        String applicationName = (String) app.getNode().getContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        // Resolve AWS account ID
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        // Base url for the Api
        String apiUrl = (String) app.getNode().tryGetContext("apiUrl");
        Validations.requireNonEmpty(apiUrl, "context variable 'apiUrl' must not be null");

        // Base url for the App
        String appUrl = (String) app.getNode().tryGetContext("appUrl");
        Validations.requireNonEmpty(appUrl, "context variable 'appUrl' must not be null");

        // Resolve AWS region for deployment
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");


        String loginPageDomainPrefix = (String) app.getNode().tryGetContext("loginPageDomainPrefix");
        Validations.requireNonEmpty(loginPageDomainPrefix, "context variable 'loginPageDomainPrefix' must not be null");

        // AWS environment (account + region)
        Environment awsEnvironment = makeEnv(accountId, region);

        // Encapsulates the deployment with application name used for naming conventions
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(applicationName,deploymentStage);

        new CognitoStack(
                app,
                "CognitoStack",
                awsEnvironment,
                applicationEnvironment,
                new CognitoStack.CognitoInputParameters(applicationName, apiUrl, appUrl, loginPageDomainPrefix)
        );

        app.synth();
    }

    /**
     * Helper method to create an AWS {@link Environment}
     * from account and region values.
     */
    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}
