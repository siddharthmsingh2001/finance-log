package com.financelog.platform;

import com.financelog.core.ApplicationEnvironment;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * Loads Postgres database outputs from SSM Parameter Store.
 *
 * <p>
 * These parameters must have been written by a previously
 * deployed {@link DatabaseConstruct}.
 * </p>
 */
public class DatabaseParameterStore {

    private DatabaseParameterStore(){
        // prevents instantiation
    }

    public static DatabaseOutputParameters load(
            Construct scope,
            ApplicationEnvironment applicationEnvironment
    ){
        return new DatabaseOutputParameters(
                get(scope, applicationEnvironment, DatabaseOutputs.ENDPOINT_ADDRESS),
                get(scope, applicationEnvironment, DatabaseOutputs.ENDPOINT_PORT),
                get(scope, applicationEnvironment, DatabaseOutputs.DATABASE_NAME),
                get(scope, applicationEnvironment, DatabaseOutputs.SECRET_ARN),
                get(scope, applicationEnvironment, DatabaseOutputs.SECURITY_GROUP_ID),
                get(scope, applicationEnvironment, DatabaseOutputs.INSTANCE_ID)
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
     *   Logical parameter key defined in {@link DatabaseOutputs}.
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
     * {@link  DatabaseConstruct}.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * dev-finance-log-endpointAddress
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
        return applicationEnvironment.prefix("database-"+key);
    }
}
