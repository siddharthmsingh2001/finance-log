package com.financelog.app;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.platform.database.BastionConstruct;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class BastionHostApp {

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

        // Resolve AWS region for deployment
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        // key naming convention: {deploymentStage}-finance-log-bastion-host
        // Resolve BastionHost private SSH keyName
        String keyName = (String) app.getNode().tryGetContext("keyName");
        Validations.requireNonEmpty(region, "context variable 'keyName' must not be null");

        // Resolve local device public IP address to connect to the BastionHost
        String localPublicIp = (String) app.getNode().tryGetContext("localPublicIp");
        Validations.requireNonEmpty(region, "context variable 'localPublicIp' must not be null");

        // AWS environment (account + region)
        Environment awsEnvironment = makeEnv(accountId, region);

        // Encapsulates the deployment with application name used for naming conventions
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(applicationName,deploymentStage);

        Stack stack = new Stack(app, "BastionHostStack",
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("bastion-stack"))
                        .env(awsEnvironment)
                        .build()
        );

        new BastionConstruct(
                stack,
                "BastionHost",
                applicationEnvironment,
                new BastionConstruct.BastionInputParameters(
                        keyName,
                        localPublicIp
                )
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
