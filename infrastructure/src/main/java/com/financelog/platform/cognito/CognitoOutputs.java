package com.financelog.platform;

/**
 * Defines the canonical set of SSM parameter keys
 * published by {@code CognitoStack}.
 *
 * <p>
 * This class represents the <b>contract</b> between:
 * </p>
 * <ul>
 *   <li>The Cognito infrastructure stack (producer).</li>
 *   <li>All consuming stacks and applications (consumers).</li>
 * </ul>
 *
 * <p>
 * Centralizing parameter keys prevents:
 * </p>
 * <ul>
 *   <li>String duplication.</li>
 *   <li>Accidental typos.</li>
 *   <li>Breaking changes due to renamed parameters.</li>
 * </ul>
 *
 * <p>
 * Any change to these constants is considered a
 * <b>breaking infrastructure change</b>.
 * </p>
 */
public class CognitoOutputs {

    /**
     * Private constructor to prevent instantiation.
     */
    private CognitoOutputs(){
        // Prevent instantiation
    }

    /** SSM parameter key for the Cognito User Pool ID. */
    public static final String PARAMETER_USER_POOL_ID = "userPoolId";
    /** SSM parameter key for the Cognito User Pool Client ID. */
    public static final String PARAMETER_USER_POOL_CLIENT_ID = "userPoolClientId";
    /**
     * SSM parameter key for the Cognito User Pool Client Secret.
     *
     * <p>
     * This value must be handled as sensitive data and
     * should never be exposed to frontend code.
     * </p>
     */
    public static final String PARAMETER_USER_POOL_CLIENT_SECRET = "userPoolClientSecret";
    /** SSM parameter key for the Cognito Hosted UI logout URL. */
    public static final String PARAMETER_LOGOUT_URL = "logoutUrl";
    /** SSM parameter key for the Cognito User Pool provider URL. */
    public static final String PARAMETER_PROVIDER_URL = "providerUrl";

}
