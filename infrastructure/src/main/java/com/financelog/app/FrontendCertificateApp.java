package com.financelog.app;

import com.financelog.domain.CertificateStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;

/**
 * CDK application entry point responsible for provisioning
 * the SSL/TLS certificate used by the frontend CloudFront distribution.
 *
 * <p>
 * Important constraints:
 * </p>
 * <ul>
 *   <li>CloudFront certificates MUST be created in us-east-1</li>
 *   <li>This app always deploys to us-east-1 regardless of application region</li>
 * </ul>
 *
 * <p>
 * This app reuses {@link CertificateStack} without modification.
 * </p>
 */
public class FrontendCertificateApp {

    public static void main(String[] args) {

        App app = new App();

        /*
         * Logical deployment stage (dev, staging, prod).
         */
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        /*
         * Application name used for consistent naming.
         */
        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        /*
         * AWS account ID.
         */
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        /*
         * Fully-qualified frontend domain name.
         * Example: app.finance-log.com
         */
        String applicationDomain = (String) app.getNode().tryGetContext("appDomain");
        Validations.requireNonEmpty(applicationDomain, "context variable 'applicationDomain' must not be null");

        /*
         * Root hosted zone domain.
         * Example: finance-log.com
         */
        String hostedZoneDomain = (String) app.getNode().tryGetContext("hostedZoneDomain");
        Validations.requireNonEmpty(hostedZoneDomain, "context variable 'hostedZoneDomain' must not be null");

        /*
         * CloudFront requires certificates to live in us-east-1.
         * This is intentionally hard-coded to avoid misconfiguration.
         */
        Environment awsEnvironment = Environment.builder()
                .account(accountId)
                .region("us-east-1")
                .build();

        ApplicationEnvironment applicationEnvironment =
                new ApplicationEnvironment(applicationName, deploymentStage);

        new CertificateStack(
                app,
                "FrontendCertificateStack",
                awsEnvironment,
                applicationEnvironment,
                applicationDomain,
                hostedZoneDomain
        );

        app.synth();
    }
}
