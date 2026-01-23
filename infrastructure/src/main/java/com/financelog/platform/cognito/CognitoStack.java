package com.financelog.platform.cognito;

import com.financelog.core.ApplicationEnvironment;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import java.util.Arrays;
import java.util.Collections;

import software.amazon.awscdk.services.cognito.*;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * CDK stack responsible for provisioning and configuring
 * AWS Cognito authentication infrastructure.
 *
 * <p>
 * This stack creates and wires together the following Cognito resources:
 * </p>
 *
 * <ul>
 *   <li>A {@link software.amazon.awscdk.services.cognito.UserPool} that represents
 *       the central identity store for application users.</li>
 *   <li>A {@link software.amazon.awscdk.services.cognito.UserPoolClient} that enables
 *       OAuth 2.0 authentication using the Authorization Code Grant.</li>
 *   <li>A {@link software.amazon.awscdk.services.cognito.UserPoolDomain} that hosts
 *       Cognito's login, signup, and logout UI.</li>
 *   <li>SSM Parameter Store entries that expose Cognito identifiers to downstream stacks.</li>
 * </ul>
 *
 * <h2>Authentication Model</h2>
 * <p>
 * This stack is designed to support:
 * </p>
 * <ul>
 *   <li>Self-service user registration (signup).</li>
 *   <li>Email-based authentication.</li>
 *   <li>OAuth 2.0 Authorization Code Grant.</li>
 *   <li>Server-side session-based authentication in the backend.</li>
 * </ul>
 *
 * <p>
 * JWT tokens issued by Cognito are exchanged and stored <b>server-side</b> by Spring Security.
 * They are <b>never exposed to the browser</b>.
 * </p>
 *
 * <h2>Environment Awareness</h2>
 * <p>
 * All resources and exported parameters are scoped to an {@link ApplicationEnvironment},
 * ensuring isolation between deployment stages such as dev, staging, and prod.
 * </p>
 */
public class CognitoStack extends Stack {

    /**
     * Logical environment abstraction combining deployment stage and application name.
     *
     * <p>
     * Used for:
     * </p>
     * <ul>
     *   <li>Generating deterministic resource names.</li>
     *   <li>Scoping SSM parameter names.</li>
     *   <li>Applying environment-wide tags.</li>
     * </ul>
     */
    private final ApplicationEnvironment applicationEnvironment;

    /**
     * Cognito User Pool acting as the authoritative identity store.
     *
     * <p>
     * All users who register or authenticate with the application
     * are represented here.
     * </p>
     */
    private final UserPool userPool;

    /**
     * OAuth 2.0 client associated with the user pool.
     *
     * <p>
     * This client is used by the backend application to:
     * </p>
     * <ul>
     *   <li>Initiate login redirects.</li>
     *   <li>Exchange authorization codes for tokens.</li>
     * </ul>
     */
    private final UserPoolClient userPoolClient;

    /**
     * Cognito-hosted domain that provides login, signup,
     * password reset, and logout pages.
     */
    private final UserPoolDomain userPoolDomain;

    /**
     * Fully qualified logout URL for the Cognito Hosted UI.
     *
     * <p>
     * Backend applications redirect users to this URL
     * during logout to ensure both local sessions and
     * Cognito sessions are terminated.
     * </p>
     */
    private final String logoutUrl;

    /**
     * Creates a new Cognito infrastructure stack.
     *
     * @param scope
     *   Parent construct in the CDK tree.
     *
     * @param constructId
     *   Logical identifier for this stack.
     *
     * @param awsEnvironment
     *   AWS account and region where resources are deployed.
     *
     * @param applicationEnvironment
     *   Logical environment descriptor (application + stage).
     *
     * @param inputParameters
     *   Application-specific configuration such as URLs
     *   and domain prefixes.
     */
    public CognitoStack(
            Construct scope,
            String constructId,
            Environment awsEnvironment,
            ApplicationEnvironment applicationEnvironment,
            CognitoInputParameters inputParameters
    ) {

        super(
                scope,
                constructId,
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("cognito-stack"))
                        .env(awsEnvironment)
                        .build()
        );

        this.applicationEnvironment = applicationEnvironment;

        /*
         * Construct the logout URL for Cognito Hosted UI.
         *
         * This URL is later used by the backend to redirect users
         * during logout, ensuring Cognito clears its session.
         */
        this.logoutUrl = String.format("https://%s.auth.%s.amazoncognito.com/logout", inputParameters.loginPageDomainPrefix, awsEnvironment.getRegion());

        // Create the core Cognito resources.
        this.userPool = createUserPool(inputParameters);
        this.userPoolClient = createUserPoolClient(inputParameters, userPool);
        this.userPoolDomain = createUserPoolDomain(inputParameters, userPool);

        // Export identifiers required by other stacks.
        createOutputParameters();

        // Apply environment-wide tags.
        applicationEnvironment.tag(this);
    }

    /**
     * Creates and configures the Cognito User Pool.
     *
     * <p>
     * This configuration enables:
     * </p>
     * <ul>
     *   <li>Self-service user signup.</li>
     *   <li>Email-based authentication.</li>
     *   <li>Email verification during registration.</li>
     *   <li>Strict password policies.</li>
     * </ul>
     *
     * @param inputParameters
     *   Application-specific configuration values.
     *
     * @return configured {@link UserPool}
     */
    private UserPool createUserPool(CognitoInputParameters inputParameters){
        UserPool userPool = UserPool.Builder.create(this, "UserPool")
                .userPoolName(inputParameters.applicationName+"-user-pool")
                .selfSignUpEnabled(true) // Allow users to register themselves
                .accountRecovery(AccountRecovery.EMAIL_ONLY) // Account recovery is limited to email-based flows
                .autoVerify(AutoVerifiedAttrs.builder().email(true).build()) // Automatically verify email addresses during signup.
                .signInAliases(SignInAliases.builder().email(true).build())  // Use email as the primary sign-in identifier
                .signInCaseSensitive(true)
                .email(UserPoolEmail.withCognito()) // Use Cognito-managed email delivery.
                .standardAttributes( StandardAttributes.builder()
                        // Define required and optional user attributes.
                        .email(StandardAttribute.builder().required(true).mutable(false).build())
                        .givenName(StandardAttribute.builder().required(true).mutable(true).build())
                        .familyName(StandardAttribute.builder().required(true).mutable(true).build())
                        .build()
                )
                .mfa(Mfa.OFF) // Disable multi-factor authentication (can be enabled later).
                .passwordPolicy( PasswordPolicy.builder() // Enforce password strength and expiry for temporary passwords.
                        .requireUppercase(true)
                        .requireLowercase(true)
                        .requireDigits(true)
                        .requireSymbols(true)
                        .minLength(7)
                        .tempPasswordValidity(Duration.days(1))
                        .build()
                )
                .build();
        userPool.applyRemovalPolicy(RemovalPolicy.DESTROY);
        return userPool;
    }

    /**
     * Creates the OAuth 2.0 client used by the backend application.
     *
     * <p>
     * This client enables the Authorization Code Grant flow,
     * which is suitable for server-side applications.
     * </p>
     *
     * @param inputParameters
     *   Application-specific configuration values.
     *
     * @return configured {@link UserPoolClient}
     */
    private UserPoolClient createUserPoolClient(CognitoInputParameters inputParameters, UserPool userPool){
        return UserPoolClient.Builder.create(this, "UserPoolClient")
                .userPool(userPool)
                .userPoolClientName(inputParameters.applicationName+"-user-pool-client")
                .generateSecret(true) // Generate a secret for secure server-to-server communication.
                .oAuth(OAuthSettings.builder() // Configure OAuth behavior.
                        .callbackUrls(Arrays.asList(
                                String.format("%s/login/oauth2/code/cognito", inputParameters.apiUrl),
                                "http://localhost:8080/login/oauth2/code/cognito"
                        ))
                        .logoutUrls(Arrays.asList(
                                inputParameters.apiUrl,
                                "http://localhost:8080"
                        ))
                        .flows(OAuthFlows.builder()
                                .authorizationCodeGrant(true)
                                .build()
                        )
                        .scopes(Arrays.asList(
                                OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.PROFILE
                        ))
                        .build()
                )
                // Restrict identity providers to Cognito itself.
                .supportedIdentityProviders(
                        Collections.singletonList(UserPoolClientIdentityProvider.COGNITO)
                )
                .build();
    }

    /**
     * Creates the Cognito Hosted UI domain.
     *
     * <p>
     * This domain hosts login, signup, and logout pages
     * managed by Cognito.
     * </p>
     *
     * @param inputParameters
     *   Configuration containing the domain prefix.
     *
     * @param userPool
     *   The user pool to associate with the domain.
     *
     * @return configured {@link UserPoolDomain}
     */
    private UserPoolDomain createUserPoolDomain(CognitoInputParameters inputParameters, UserPool userPool){
        return UserPoolDomain.Builder.create(this, "UserPoolDomain")
                .userPool(userPool)
                .cognitoDomain(CognitoDomainOptions.builder()
                        .domainPrefix(inputParameters.loginPageDomainPrefix)
                        .build()
                )
                .build();
    }

    /**
     * Publishes Cognito identifiers to AWS Systems Manager
     * Parameter Store.
     *
     * <p>
     * These parameters form a stable contract between
     * the Cognito stack and downstream stacks such as
     * backend services.
     * </p>
     */
    private void createOutputParameters(){
        putParameter("UserPoolId", CognitoOutputs.PARAMETER_USER_POOL_ID, userPool.getUserPoolId());
        putParameter("UserPoolClientId", CognitoOutputs.PARAMETER_USER_POOL_CLIENT_ID, userPoolClient.getUserPoolClientId());
        putParameter("LogoutUrl", CognitoOutputs.PARAMETER_LOGOUT_URL, logoutUrl);
        putParameter("ProviderUrl", CognitoOutputs.PARAMETER_PROVIDER_URL, userPool.getUserPoolProviderUrl());
        putParameter("UserPoolClientSecret", CognitoOutputs.PARAMETER_USER_POOL_CLIENT_SECRET,userPoolClient.getUserPoolClientSecret().unsafeUnwrap());
    }

    /**
     * Writes a single string value to SSM Parameter Store.
     *
     * @param constructId
     *   Logical identifier within the CDK construct tree.
     *
     * @param key
     *   Logical parameter key.
     *
     * @param value
     *   Value to store.
     */
    private void putParameter(String constructId, String key, String value){
        StringParameter.Builder.create(this, constructId)
                .parameterName(createParameterName(applicationEnvironment, key))
                .stringValue(value)
                .build();
    }

    /**
     * Generates a fully qualified SSM parameter name
     * scoped to the application environment.
     *
     * @param applicationEnvironment
     *   Logical environment descriptor.
     *
     * @param parameterKey
     *   Logical parameter identifier.
     *
     * @return fully qualified parameter name
     */
    @NotNull
    private static String createParameterName(ApplicationEnvironment applicationEnvironment, String parameterKey){
        return  applicationEnvironment.prefix("cognito-" + parameterKey);
    }

    /**
     * Immutable configuration object used to pass
     * application-specific values into the Cognito stack.
     */
    public static class CognitoInputParameters{
        private final String applicationName;
        private final String apiUrl;
        private final String loginPageDomainPrefix;

        public CognitoInputParameters(String applicationName, String apiUrl, String loginPageDomainPrefix) {
            this.applicationName = applicationName;
            this.apiUrl = apiUrl;
            this.loginPageDomainPrefix = loginPageDomainPrefix;
        }
    }

}

