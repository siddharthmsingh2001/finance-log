package com.financelog.app;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import com.financelog.core.SpringProfile;
import com.financelog.core.Validations;
import com.financelog.network.NetworkOutputParameters;
import com.financelog.network.NetworkParameterStore;
import com.financelog.platform.cognito.CognitoOutputParameters;
import com.financelog.platform.cognito.CognitoParameterStore;
import com.financelog.platform.database.DatabaseOutputParameters;
import com.financelog.platform.database.DatabaseParameterStore;
import com.financelog.service.ServiceConstruct;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * CDK entry point (App) responsible for deploying the ECS Service stack.
 *
 * <p>
 * This class represents the <strong>application-level bootstrap</strong>
 * for the Service layer of the infrastructure. It does not create AWS
 * resources directly, but instead:
 * </p>
 *
 * <ul>
 *   <li>Reads deployment configuration from CDK context</li>
 *   <li>Resolves AWS account and region</li>
 *   <li>Constructs environment-specific naming and tagging</li>
 *   <li>Instantiates the Service stack</li>
 *   <li>Loads network outputs produced by the Network stack</li>
 * </ul>
 *
 * <p>
 * This App is intentionally separated from the Network App to enforce
 * loose coupling between infrastructure layers. The Service stack
 * depends on the Network stack only via exported parameters.
 * </p>
 *
 * <p>
 * Typical invocation:
 * </p>
 *
 * <pre>
 * cdk deploy ServiceStack \
 *   -c environmentName=dev \
 *   -c applicationName=finance-log \
 *   -c accountId=123456789012 \
 *   -c region=ap-south-1 \
 *   -c springProfile=dev
 * </pre>
 */
public class ServiceApp {

    /**
     * Entry point for the CDK application.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     *
     * <ol>
     *   <li>Initialize the CDK {@link App}</li>
     *   <li>Read and validate required context variables</li>
     *   <li>Resolve logical deployment environment (dev/staging/prod)</li>
     *   <li>Create an AWS {@link Environment}</li>
     *   <li>Derive application-level naming and tagging</li>
     *   <li>Create the Service {@link Stack}</li>
     *   <li>Load Network stack outputs from SSM Parameter Store</li>
     * </ol>
     */
    public static void main(String[] args) {

        // Root CDK application
        App app = new App();

        /*
         * Logical deployment stage (e.g. dev, staging, prod).
         * Used for naming, tagging, and parameter namespacing.
         */
        String stageName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(stageName, "context variable 'environmentName' must not be null");
        DeploymentStage deploymentStage = DeploymentStage.from(stageName);

        /*
         * Application name (e.g. finance-log).
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
         * Spring profile to activate inside the container.
         * Typically, matches the deployment stage (dev, prod).
         */
        String profileName = (String) app.getNode().tryGetContext("springProfile");
        Validations.requireNonEmpty(profileName, "context variable 'springProfile' must not be null");
        SpringProfile springProfile = SpringProfile.from(profileName);

        /*
         * ECR Image Repository Name for main application
         */
        String repositoryName = (String) app.getNode().tryGetContext("repositoryName");
        Validations.requireNonEmpty(repositoryName, "context variable 'repositoryName' must not be null");

        /*
         * ECR Image Repository Tag
         */
        String imageTag = (String) app.getNode().tryGetContext("imageTag");
        Validations.requireNonEmpty(imageTag, "context variable 'imageTag' must not be null");

        /*
         * AWS region for deployment.
         */
        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

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
         * Stack that hosts all ECS service-related resources.
         *
         * The stack name is environment-aware, e.g.:
         * - dev-finance-log-service-stack
         * - prod-finance-log-service-stack
         */
        Stack serviceStack = new Stack( app, "ServiceStack",
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("service-stack") ) // dev-service-stack,
                        .env(awsEnvironment)
                        .build()
        );

        /*
         * Load network-level identifiers produced by the Network stack.
         *
         * These values are retrieved from SSM Parameter Store and include:
         * - VPC ID
         * - Subnet IDs
         * - ECS cluster name
         * - Load balancer listeners
         *
         * This avoids direct CloudFormation stack dependencies.
         */
        NetworkOutputParameters networkOutputParameters = NetworkParameterStore.load(serviceStack, deploymentStage);

        /*
         * Load database outputs produced by the Database stack.
         * These values are resolved from SSM Parameter Store.
         */
        DatabaseOutputParameters databaseOutputParameters = DatabaseParameterStore.load(serviceStack,applicationEnvironment);

        /*
         * Load Cognito outputs produced by the Cognito stack.
         */
        CognitoOutputParameters cognitoOutputParameters = CognitoParameterStore.load(serviceStack,applicationEnvironment);



        /*
         * Instantiate the ServiceConstruct, which acts as the architectural orchestrator.
         * * It takes the existing Network foundation and "plugs in" the application service.
         * Responsibilities include:
         * - Defining the ECS Fargate Task Definition and Service
         * - Configuring Load Balancer listeners and Target Groups
         * - Setting up CloudWatch logging and IAM execution roles
         */
        ServiceConstruct serviceConstruct = new ServiceConstruct(
                serviceStack,
                "Service",
                awsEnvironment,
                applicationEnvironment,
                new ServiceConstruct.ServiceInputParameters(
                        new ServiceConstruct.DockerImageSource(repositoryName,imageTag),
                        Arrays.asList(
                                databaseOutputParameters.getSecurityGroupId()
                        ),
                        environmentVariables(
                                serviceStack,
                                springProfile.getName(),
                                databaseOutputParameters,
                                cognitoOutputParameters
                        )
                ),
                networkOutputParameters
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

    static Map<String, String> environmentVariables(
            Construct scope,
            String springProfile,
            DatabaseOutputParameters databaseOutputParameters,
            CognitoOutputParameters cognitoOutputParameters
    ) {
        Map<String, String> vars = new HashMap<>();
        // --- Spring Profile ---
        vars.put("SPRING_PROFILES_ACTIVE", springProfile);
        // --- Database Configuration ---
        vars.put(
                "SPRING_DATASOURCE_URL",
                String.format(
                        "jdbc:mysql://%s:%s/%s",
                        databaseOutputParameters.getEndpointAddress(),
                        databaseOutputParameters.getEndpointPort(),
                        databaseOutputParameters.getDatabaseName())
        );
        ISecret databaseSecret = Secret.fromSecretCompleteArn(scope,"DatabaseSecret", databaseOutputParameters.getSecretArn());
        // --- Datasource Configuration ---
        vars.put("SPRING_DATASOURCE_USERNAME", databaseSecret.secretValueFromJson("username").unsafeUnwrap());
        vars.put("SPRING_DATASOURCE_PASSWORD", databaseSecret.secretValueFromJson("password").unsafeUnwrap());
        // --- Cognito Configuration ---
        vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.getUserPoolClientId());
        vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.getUserPoolClientSecret());
        vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.getProviderUrl());
        // --- App Configuration ---
        vars.put("APP_URL", "https://app.finance-log.com");
        return vars;
    }
}