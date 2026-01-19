package com.financelog.app;


import com.financelog.domain.FrontendDomainStack;
import software.amazon.awscdk.*;
import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;

/**
 * CDK application entry point responsible for configuring
 * DNS routing for the app domain.
 *
 * <p>
 * This application:
 * <ul>
 *   <li>Constructs an {@link ApplicationEnvironment}</li>
 *   <li>Instantiates the {@link FrontendDomainStack}</li>
 *   <li>Synthesizes the CloudFormation template</li>
 * </ul>
 *
 * <p>
 * This app assumes that:
 * <ul>
 *   <li>The public Route 53 hosted zone already exists</li>
 *   <li>A Cloudfront Distribution has already been provisioned</li>
 *   <li>SSM Parameter already has CDN name and id</li>
 * </ul>
 */
public class FrontendDomainApp {

    public static void main(String[] args) {

        App app = new App();

        /*
         * Deployment stage (dev, staging, prod).
         */
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        /*
         * Application name.
         */
        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        /*
         * AWS account ID.
         */
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        /*
         * Hosted zone root domain (finance-log.com).
         */
        String hostedZoneDomain = (String) app.getNode().tryGetContext("hostedZoneDomain");
        Validations.requireNonEmpty(hostedZoneDomain, "context variable 'hostedZoneDomain' must not be null");

        /*
         * Frontend fully-qualified domain name.
         * Example: app.finance-log.com
         */
        String frontendDomainName = (String) app.getNode().tryGetContext("appDomain");
        Validations.requireNonEmpty(frontendDomainName, "context variable 'frontendDomainName' must not be null");

        Environment awsEnvironment = Environment.builder()
                .account(accountId)
                .region("us-east-1") // Route53 is global, region doesn't matter
                .build();

        ApplicationEnvironment applicationEnvironment =
                new ApplicationEnvironment(applicationName, deploymentStage);

        new FrontendDomainStack(
                app,
                "FrontendDomainStack",
                awsEnvironment,
                applicationEnvironment,
                hostedZoneDomain,
                frontendDomainName
        );

        app.synth();
    }
}
