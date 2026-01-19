package com.financelog.app;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.service.FrontendStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/**
 * Entry point for the CDK application to deploy the React frontend infrastructure.
 * * <p>This app fetches required configuration from the CDK context (cdk.json)
 * and initializes the {@link FrontendStack}. It enforces the deployment to
 * <b>us-east-1</b> because AWS Certificate Manager (ACM) certificates for
 * CloudFront distributions must reside in that specific region.</p>
 * * <b>Required Context Variables:</b>
 * <ul>
 * <li>{@code environmentName}: The deployment stage (e.g., dev, prod).</li>
 * <li>{@code applicationName}: Name of the project (e.g., finance-log).</li>
 * <li>{@code accountId}: The target AWS account ID.</li>
 * <li>{@code sslCertificateArnFrontend}: The ARN of the ACM certificate for the custom domain.</li>
 * </ul>
 */
public class FrontendApp {

    public static void main(String[] args) {

        App app = new App();

        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        /*
         * Certificate ARN created by FrontendCertificateApp.
         */
        String certificateArn = (String) app.getNode().tryGetContext("sslCertificateArnFrontend");
        Validations.requireNonEmpty(certificateArn, "context variable 'sslCertificateArnFrontend' must not be null");

        /*
         * CloudFront must run in us-east-1.
         */
        Environment awsEnvironment = Environment.builder()
                .account(accountId)
                .region("us-east-1")
                .build();

        ApplicationEnvironment applicationEnvironment =
                new ApplicationEnvironment(applicationName, deploymentStage);

        new FrontendStack(
                app,
                "FrontendStack",
                awsEnvironment,
                applicationEnvironment,
                certificateArn
        );

        app.synth();

    }

}
