package com.financelog.app;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.domain.DomainStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/**
 * CDK application entry point responsible for configuring
 * DNS routing for the application domain.
 *
 * <p>
 * This application:
 * <ul>
 *   <li>Reads deployment configuration from CDK context</li>
 *   <li>Validates required inputs</li>
 *   <li>Constructs an {@link ApplicationEnvironment}</li>
 *   <li>Instantiates the {@link DomainStack}</li>
 *   <li>Synthesizes the CloudFormation template</li>
 * </ul>
 *
 * <p>
 * This app assumes that:
 * <ul>
 *   <li>The public Route 53 hosted zone already exists</li>
 *   <li>An Application Load Balancer has already been provisioned</li>
 *   <li>Network stack outputs are available in SSM Parameter Store</li>
 * </ul>
 */
public class DomainApp {

    /**
     * Main entry point for the CDK application.
     *
     * @param args command-line arguments (not used by CDK)
     */
    public static void main(String[] args) {

        /*
         * Root CDK application.
         * Acts as the construct tree root for all stacks.
         */
        App app = new App();

        /*
         * Logical deployment stage (e.g. dev, staging, prod).
         * Used for naming, tagging, and parameter namespacing.
         */
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        /*
         * Application name .
         * Forms the base for all resource names.
         */
        String applicationName = (String) app.getNode().getContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        /*
         * AWS account ID into which the stack will be deployed.
         */
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        /*
         * AWS region for deployment.
         */
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        /*
         * The exact hostname that the frontend will connect to via HTTPs e.g. api.finance-log.com OR app.finance-log.com
         */
        String applicationDomain = (String) app.getNode().tryGetContext("applicationDomain");
        Validations.requireNonEmpty(applicationDomain, "context variable 'applicationDomain' must not be null");

        /*
         * The authoritative DNS zone in Route 53 which is a public hosted zone (aka Domain Name) which is finance-log.com
         */
        String hostedZoneDomain = (String) app.getNode().tryGetContext("hostedZoneDomain");
        Validations.requireNonEmpty(hostedZoneDomain, "context variable 'hostedZoneDomain' must not be null");

        // AWS environment (account + region)
        Environment awsEnvironment = makeEnv(accountId, region);

        /*
         * ApplicationEnvironment encapsulates:
         * - application name
         * - deployment stage
         *
         * It is used to generate consistent names and tags.
         */
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(applicationName, deploymentStage);

        /*
         * Stack responsible for associating a Custom Domain for the ELB.
         */
        new DomainStack(
                app,
                "DomainStack",
                awsEnvironment,
                applicationEnvironment,
                hostedZoneDomain,
                applicationDomain
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

