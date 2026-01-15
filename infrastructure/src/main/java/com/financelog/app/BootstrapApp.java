package com.financelog.app;

import com.financelog.core.Validations;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/**
 * An empty app that we can use for bootstrapping the CDK
 * with the "cdk bootstrap" command
 */
public class BootstrapApp {

    public static void main(String[] args) {

        // CDK App represents the root of the construct tree
        App app = new App();

        // Read required context variables provided via:
        // cdk deploy -c accountId=... -c region=... -c applicationName=...
        // OR
        // through cdk.json
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        // Explicitly define the AWS environment (account + region)
        // to avoid accidental cross-account or cross-region deployments
        Environment awsEnvironment = makeEnv(accountId, region);

        // An empty Stack
        Stack bootstrapStack = new Stack(app, "BootstrapStack",
            StackProps.builder()
                .env(awsEnvironment)
                .build());
    }

    /**
     * Creates an AWS {@link Environment} object for CDK stacks.
     *
     * <p>
     * Explicit environment binding ensures that:
     * </p>
     *
     * <ul>
     *   <li>Stacks are deployed to the intended AWS account</li>
     *   <li>Resources are created in the correct region</li>
     *   <li>Cross-environment ambiguity is avoided</li>
     * </ul>
     *
     * @param account AWS account ID
     * @param region  AWS region
     * @return a fully configured {@link Environment} instance
     */
    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }

}
