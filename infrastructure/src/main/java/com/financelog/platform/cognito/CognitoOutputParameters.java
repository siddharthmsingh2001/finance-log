package com.financelog.platform.cognito;

/**
 * Immutable value object containing Cognito-related
 * infrastructure identifiers.
 *
 * <p>
 * Instances of this class are typically created by
 * {@link CognitoParameterStore} and injected into
 * backend stacks or application configuration.
 * </p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a strongly typed alternative to raw strings.</li>
 *   <li>Encapsulate Cognito infrastructure dependencies.</li>
 *   <li>Improve readability and maintainability of consuming code.</li>
 * </ul>
 *
 * <p>
 * All values contained in this class originate from
 * AWS Systems Manager Parameter Store and represent
 * already-provisioned infrastructure.
 * </p>
 */
public class CognitoOutputParameters {

    /** Identifier of the Cognito User Pool. */
    private final String userPoolId;

    /** Identifier of the Cognito User Pool Client. */
    private final String userPoolClientId;

    /**
     * Secret associated with the Cognito User Pool Client.
     *
     * <p>
     * This secret is used by backend applications
     * during OAuth 2.0 token exchanges and must be
     * treated as sensitive information.
     * </p>
     */
    private final String userPoolClientSecret;

    /** Logout URL for the Cognito Hosted UI. */
    private final String logoutUrl;

    /** OpenID Connect provider URL for the Cognito User Pool. */
    private final String providerUrl;

    /**
     * Creates a fully populated Cognito output object.
     *
     * @param userPoolId
     *   Cognito User Pool identifier.
     *
     * @param userPoolClientId
     *   Cognito User Pool Client identifier.
     *
     * @param userPoolClientSecret
     *   Client secret used for OAuth token exchange.
     *
     * @param logoutUrl
     *   Hosted UI logout URL.
     *
     * @param providerUrl
     *   OpenID Connect provider URL.
     */
    public CognitoOutputParameters(
            String userPoolId,
            String userPoolClientId,
            String userPoolClientSecret,
            String logoutUrl,
            String providerUrl
    ) {
        this.userPoolId = userPoolId;
        this.userPoolClientId = userPoolClientId;
        this.userPoolClientSecret = userPoolClientSecret;
        this.logoutUrl = logoutUrl;
        this.providerUrl = providerUrl;
    }

    /** @return Cognito User Pool ID */
    public String getUserPoolId() {
        return userPoolId;
    }

    /** @return Cognito User Pool Client ID */
    public String getUserPoolClientId() {
        return userPoolClientId;
    }

    /** @return Cognito User Pool Client Secret */
    public String getUserPoolClientSecret() {
        return userPoolClientSecret;
    }

    /** @return Cognito Hosted UI logout URL */
    public String getLogoutUrl() {
        return logoutUrl;
    }

    /** @return Cognito OpenID Connect provider URL */
    public String getProviderUrl() {
        return providerUrl;
    }
}