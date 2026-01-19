package com.financelog.app;

import com.financelog.core.DeploymentStage;
import com.financelog.core.Validations;
import com.financelog.network.NetworkConstruct;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/**
 * CDK application entry point responsible for deploying
 * the network infrastructure stack.
 *
 * <p>
 * This application provisions all foundational network resources
 * required by the system, including:
 * </p>
 *
 * <ul>
 *   <li>VPC and subnet topology</li>
 *   <li>Application Load Balancer</li>
 *   <li>ECS Cluster</li>
 * </ul>
 *
 * <p>
 * Architectural role:
 * </p>
 *
 * <ul>
 *   <li>Follows the "one app per stack" deployment model</li>
 *   <li>Can be deployed independently of other stacks</li>
 *   <li>Acts as the root provider of network identifiers via SSM</li>
 * </ul>
 *
 * <p>
 * This application is typically deployed early in the lifecycle
 * of an environment and consumed by downstream stacks such as
 * ECS services or application layers.
 * </p>
 */
public class NetworkApp {

    /**
     * Application entry point.
     *
     * <p>
     * Execution flow:
     * </p>
     *
     * <ol>
     *   <li>Create the CDK application root</li>
     *   <li>Read and validate required context variables</li>
     *   <li>Resolve the deployment stage</li>
     *   <li>Configure optional network features (e.g. HTTPS)</li>
     *   <li>Define the AWS deployment environment</li>
     *   <li>Instantiate the network stack</li>
     *   <li>Synthesize the CloudFormation template</li>
     * </ol>
     *
     * @param args command-line arguments (not used directly)
     */
    public static void main(String[] args) {
        // Root of the CDK construct tree
        App app = new App();

        // Resolve the logical deployment stage (e.g. dev, staging, prod)
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        // Resolve AWS region for deployment
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        // Resolve AWS account ID
        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        // Collect optional network input parameters
        NetworkConstruct.NetworkInputParameters inputParameters = new NetworkConstruct.NetworkInputParameters();

        // Optional SSL certificate for HTTPS support
        String sslCertificateArn = (String) app.getNode().tryGetContext("sslCertificateArnBackend");
        if (sslCertificateArn != null && !sslCertificateArn.isBlank()) {
            inputParameters.withSslCertificateArn(sslCertificateArn);
        }

        // Explicitly define the AWS deployment environment
        Environment awsEnvironment = makeEnv(accountId, region);

        // Define the stack responsible for network infrastructure
        Stack networkStack = new Stack( app, "NetworkStack",
                StackProps.builder()
                        .stackName(deploymentStage.getName()+"-network-stack")
                        .env(awsEnvironment)
                        .build()
        );

        // Attach the network construct to the stack
        NetworkConstruct networkConstruct = new NetworkConstruct(
                networkStack,
                "Network",
                deploymentStage,
                inputParameters
        );

        app.synth();

    }

    /**
     * Creates an AWS {@link Environment} definition for CDK stacks.
     *
     * <p>
     * Binding stacks explicitly to an account and region:
     * </p>
     *
     * <ul>
     *   <li>Prevents accidental cross-account deployments</li>
     *   <li>Ensures predictable resource placement</li>
     *   <li>Aligns with multi-environment deployment strategies</li>
     * </ul>
     *
     * @param account AWS account ID
     * @param region  AWS region
     * @return configured {@link Environment}
     */
    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }

}