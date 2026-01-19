package com.financelog.app;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import com.financelog.core.Validations;
import com.financelog.repository.RepositoryConstruct;

/**
 * CDK application entry point for provisioning the ECR repository stack.
 *
 * <p>
 * This class represents a standalone CDK app whose sole responsibility
 * is to deploy container registry infrastructure.
 * </p>
 *
 * <p>
 * Design rationale:
 * </p>
 *
 * <ul>
 *   <li>Follows the "one app per stack" pattern</li>
 *   <li>Allows independent deployment of the repository</li>
 *   <li>Prevents tight coupling with compute or networking stacks</li>
 * </ul>
 *
 * <p>
 * This app is typically deployed before any ECS or CI/CD stacks
 * that depend on the repository.
 * </p>
 *
 */
public class RepositoryApp {

    /**
     * Application entry point.
     *
     * <p>
     * Execution flow:
     * </p>
     *
     * <ol>
     *   <li>Read required context variables</li>
     *   <li>Validate mandatory configuration</li>
     *   <li>Create AWS environment configuration</li>
     *   <li>Instantiate the repository stack</li>
     *   <li>Attach the repository construct</li>
     *   <li>Synthesize the CloudFormation template</li>
     * </ol>
     *
     * @param args command-line arguments (not used directly)
     */
    public static void main(String[] args) {

        // CDK App represents the root of the construct tree
        App app = new App();

        // Read required context variables provided via:
        // cdk deploy -c accountId=... -c region=... -c applicationName=...
        // OR
        // through cdk.json
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        // Explicitly define the AWS environment (account + region)
        // to avoid accidental cross-account or cross-region deployments
        Environment awsEnvironment = makeEnv(accountId, region);

        // Define the stack responsible for ECR infrastructure
        Stack repositoryStack = new Stack(app, "RepositoryStack",
                StackProps.builder()
                        .stackName(applicationName+"-repository-stack")
                        .env(awsEnvironment)
                        .build()
        );

        // Define the Construct responsible for ECR infrastructure
        RepositoryConstruct repositoryConstruct = new RepositoryConstruct(
                repositoryStack,
                "Repository",
                new RepositoryConstruct.RepositoryInputParameters(
                        applicationName,
                        accountId,
                        10,
                        false
                ));

        app.synth();
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
