package com.financelog.platform;

/**
 * Immutable value object representing an existing MySQL database.
 *
 * <p>
 * All values originate from SSM Parameter Store and
 * describe already-provisioned infrastructure.
 * </p>
 */
public class DatabaseOutputParameters {

    private final String endpointAddress;
    private final String endpointPort;
    private final String databaseName;
    private final String secretArn;
    private final String securityGroupId;
    private final String instanceId;

    public DatabaseOutputParameters(String endpointAddress, String endpointPort, String databaseName, String secretArn, String securityGroupId, String instanceId) {
        this.endpointAddress = endpointAddress;
        this.endpointPort = endpointPort;
        this.databaseName = databaseName;
        this.secretArn = secretArn;
        this.securityGroupId = securityGroupId;
        this.instanceId = instanceId;
    }

    /** @return database endpoint hostname */
    public String getEndpointAddress() {
        return endpointAddress;
    }

    /** @return database port */
    public String getEndpointPort() {
        return endpointPort;
    }

    /** @return database name */
    public String getDatabaseName() {
        return databaseName;
    }

    /** @return ARN of the credentials secret */
    public String getSecretArn() {
        return secretArn;
    }

    /** @return security group ID attached to the database */
    public String getSecurityGroupId() {
        return securityGroupId;
    }

    /** @return RDS instance identifier */
    public String getInstanceId() {
        return instanceId;
    }

}
