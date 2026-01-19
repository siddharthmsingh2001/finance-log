package com.financelog.app;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.service.FrontendStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

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
