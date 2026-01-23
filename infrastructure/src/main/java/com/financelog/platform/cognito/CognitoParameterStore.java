package com.financelog.platform.cognito;


import com.financelog.core.ApplicationEnvironment;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * Utility class responsible for loading Cognito-related
 * infrastructure outputs from AWS Systems Manager
 * Parameter Store.
 *
 * <p>
 * This class represents the <b>read side</b> of the contract
 * established by {@code CognitoStack}.
 * </p>
 *
 * <h2>Responsibility</h2>
 * <ul>
 *   <li>Resolve environment-scoped SSM parameters.</li>
 *   <li>Assemble them into a strongly typed
 *       {@link CognitoOutputParameters} object.</li>
 *   <li>Provide a clean abstraction for downstream stacks
 *       and applications.</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>This class contains <b>no AWS resource creation</b>.</li>
 *   <li>All methods are {@code static}.</li>
 *   <li>The class cannot be instantiated.</li>
 *   <li>The parameter naming scheme must exactly match
 *       the one used by {@code CognitoStack}.</li>
 * </ul>
 *
 * <p>
 * This separation ensures a clear boundary between
 * infrastructure provisioning and infrastructure consumption.
 * </p>
 */
public final class CognitoParameterStore {

    private CognitoParameterStore() {
        // Utility class
    }

    /**
     * Loads all Cognito-related output parameters
     * for a given deployment stage.
     *
     * <p>
     * This method retrieves values written by
     * {@code CognitoStack} and assembles them into a
     * {@link CognitoOutputParameters} object.
     * </p>
     *
     * <p>
     * Typical usage:
     * </p>
     *
     * <pre>
     * CognitoOutputParameters cognito =
     *     CognitoParameterStore.load(this, DeploymentStage.DEV);
     * </pre>
     *
     * @param scope
     *   CDK construct used for parameter resolution.
     *
     * @param applicationEnvironment
     *   Application Environment that has stage and application name (e.g. dev-finance-log).
     *
     * @return a fully populated {@link CognitoOutputParameters}
     *         instance containing Cognito identifiers.
     */
    public static CognitoOutputParameters load(
            Construct scope,
            ApplicationEnvironment applicationEnvironment
    ) {
        return new CognitoOutputParameters(
                get(scope, applicationEnvironment, CognitoOutputs.PARAMETER_USER_POOL_ID),
                get(scope, applicationEnvironment, CognitoOutputs.PARAMETER_USER_POOL_CLIENT_ID),
                get(scope, applicationEnvironment, CognitoOutputs.PARAMETER_USER_POOL_CLIENT_SECRET),
                get(scope, applicationEnvironment, CognitoOutputs.PARAMETER_LOGOUT_URL),
                get(scope, applicationEnvironment, CognitoOutputs.PARAMETER_PROVIDER_URL)
        );
    }

    /**
     * Retrieves a single string parameter from
     * AWS Systems Manager Parameter Store.
     *
     * <p>
     * This method assumes that the parameter exists
     * and will fail synthesis or deployment if it does not.
     * </p>
     *
     * @param scope
     *   CDK construct used for resolution.
     *
     * @param applicationEnvironment
     *   ApplicationEnvironment used for parameter scoping.
     *
     * @param key
     *   Logical parameter key defined in {@link CognitoOutputs}.
     *
     * @return resolved parameter value.
     */
    private static String get(Construct scope, ApplicationEnvironment applicationEnvironment, String key) {
        return StringParameter.fromStringParameterName(
                scope,
                key,
                parameterName(applicationEnvironment, key)
        ).getStringValue();
    }

    /**
     * Constructs the fully qualified SSM parameter name
     * for a given deployment stage and parameter key.
     *
     * <p>
     * The resulting name must match exactly the naming
     * scheme used when parameters are written by
     * {@code CognitoStack}.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * dev-finance-log-cognito-userPoolId
     * </pre>
     *
     * @param applicationEnvironment
     *   Application Environment.
     *
     * @param key
     *   Logical parameter key.
     *
     * @return fully qualified SSM parameter name.
     */
    private static String parameterName(ApplicationEnvironment applicationEnvironment, String key) {
        return applicationEnvironment.prefix("cognito-"+key);
    }
}
