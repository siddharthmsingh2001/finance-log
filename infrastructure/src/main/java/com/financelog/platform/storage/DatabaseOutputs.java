package com.financelog.platform.storage;


/**
 * Defines the SSM parameter keys exported by the MySQL database stack.
 *
 * <p>
 * These keys are written by {@link DatabaseConstruct}
 * and consumed via {@link DatabaseParameterStore}.
 * </p>
 */
public class DatabaseOutputs {

    private DatabaseOutputs(){
        // prevent instantiation
    }

    public static final String ENDPOINT_ADDRESS = "endpointAddress";
    public static final String ENDPOINT_PORT = "endpointPort";
    public static final String DATABASE_NAME = "databaseName";
    public static final String SECURITY_GROUP_ID = "securityGroupId";
    public static final String SECRET_ARN = "secretArn";
    public static final String INSTANCE_ID = "instanceId";

}
