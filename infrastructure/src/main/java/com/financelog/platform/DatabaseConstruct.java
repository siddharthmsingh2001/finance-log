package com.financelog.platform;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.network.NetworkOutputParameters;
import com.financelog.network.NetworkParameterStore;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnDeletionPolicy;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.rds.CfnDBInstance;
import software.amazon.awscdk.services.rds.CfnDBSubnetGroup;
import software.amazon.awscdk.services.secretsmanager.CfnSecretTargetAttachment;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import java.util.Collections;
import java.util.Objects;


/**
 * Provisions a MySQL RDS instance inside isolated subnets.
 *
 * <p>
 * This construct is responsible only for infrastructure creation
 * and exporting its outputs to SSM Parameter Store.
 * </p>
 *
 * <p>
 * The database credentials are stored in AWS Secrets Manager
 * with fields {@code username} and {@code password}.
 * </p>
 */
public class DatabaseConstruct extends Construct {

    private final CfnDBInstance dbInstance;
    private final CfnSecurityGroup securityGroup;
    private final ISecret databaseSecret;
    private final ApplicationEnvironment applicationEnvironment;

    public DatabaseConstruct(
            Construct scope,
            String constructId,
            ApplicationEnvironment applicationEnvironment,
            DatabaseInputParameters inputParameters
    ){
        super(scope, constructId);
        NetworkOutputParameters outputParameters = NetworkParameterStore.load(scope,applicationEnvironment.getDeploymentStage());
        this.applicationEnvironment = applicationEnvironment;
        this.securityGroup = createSecurityGroup(outputParameters);
        this.databaseSecret = createSecret();
        CfnDBSubnetGroup subnetGroup = createDBSubnetGroup(outputParameters);
        this.dbInstance = createInstance(outputParameters, inputParameters, subnetGroup);
        this.dbInstance.addDependency(subnetGroup);
        attachSecret();
        createOutputParameters();
    }

    /**
     * Creates the security group for the database.
     */
    private CfnSecurityGroup createSecurityGroup(NetworkOutputParameters outputParameters){
        return CfnSecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
                .vpcId(outputParameters.getVpcId())
                .groupDescription("Security Group for MySQL Database")
                .groupName(applicationEnvironment.prefix("db-sg"))
                .build();
    }

    /**
     * Creates the database credentials secret.
     */
    private ISecret createSecret(){
        String username = sanitizeDbParameterName(applicationEnvironment.prefix("dbUser"));
        return Secret.Builder.create(this, "DatabaseSecret")
                .secretName(applicationEnvironment.prefix("db-secret"))
                .description("Credentials to the RDS instance")
                .removalPolicy(RemovalPolicy.DESTROY)
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate(String.format("{\"username\":\"%s\"}", username))
                        .generateStringKey("password")
                        .passwordLength(32)
                        .excludeCharacters("@/\\\" ")
                        .build()
                ).build();
    }

    /**
     * Creates the subnet group for the database.
     */
    private CfnDBSubnetGroup createDBSubnetGroup(NetworkOutputParameters outputParameters){
        return CfnDBSubnetGroup.Builder.create(this, "DatabaseSubnetGroup")
                .dbSubnetGroupDescription("Subnet Group for the DB Instance")
                .dbSubnetGroupName(applicationEnvironment.prefix("db-subnet-group"))
                .subnetIds(outputParameters.getIsolatedSubnets())
                .build();
    }

    /**
     * Creates the MySQL RDS instance.
     */
    private CfnDBInstance createInstance(
            NetworkOutputParameters outputParameters,
            DatabaseInputParameters inputParameters,
            CfnDBSubnetGroup subnetGroup
    ){
        CfnDBInstance db = CfnDBInstance.Builder.create(this, "MySQLInstance")
                .dbInstanceIdentifier(applicationEnvironment.prefix("database"))
                .allocatedStorage(String.valueOf(inputParameters.storageInGb))
                .vpcSecurityGroups(Collections.singletonList(securityGroup.getAttrGroupId()))
                .dbSubnetGroupName(subnetGroup.getDbSubnetGroupName())
                .dbInstanceClass(inputParameters.instanceClass)
                .publiclyAccessible(false)
                .engine("mysql")
                .engineVersion(inputParameters.mysqlVersion)
                .dbName(sanitizeDbParameterName(applicationEnvironment.prefix("database")))
                .deletionProtection(false)
                .deleteAutomatedBackups(true)
                .masterUsername(databaseSecret.secretValueFromJson("username").unsafeUnwrap())
                .masterUserPassword(databaseSecret.secretValueFromJson("password").unsafeUnwrap())
                .build();
        db.getCfnOptions().setDeletionPolicy(CfnDeletionPolicy.DELETE);
        return db;
    }


    /**
     * Attaches the secret to the RDS instance.
     */
    public void attachSecret(){
        CfnSecretTargetAttachment.Builder.create(this, "SecretTargetAttachment")
                .secretId(databaseSecret.getSecretArn())
                .targetId(dbInstance.getRef())
                .targetType("AWS::RDS::DBInstance")
                .build();
    }

    private void createOutputParameters(){
        putParameter("EndpointAddress", DatabaseOutputs.ENDPOINT_ADDRESS, dbInstance.getAttrEndpointAddress());
        putParameter("EndpointPort", DatabaseOutputs.ENDPOINT_PORT, dbInstance.getAttrEndpointPort());
        putParameter("DatabaseName", DatabaseOutputs.DATABASE_NAME, dbInstance.getDbName());
        putParameter("SecurityGroupId", DatabaseOutputs.SECURITY_GROUP_ID, securityGroup.getAttrGroupId());
        putParameter("Secret", DatabaseOutputs.SECRET_ARN, databaseSecret.getSecretArn());
        putParameter("InstanceId", DatabaseOutputs.INSTANCE_ID, dbInstance.getDbInstanceIdentifier());
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
        return  applicationEnvironment.prefix("database-" + parameterKey);
    }

    private String sanitizeDbParameterName(String value) {
        return value
                .replaceAll("[^a-zA-Z0-9_]", "")
                .replaceAll("^[^a-zA-Z]", "a");
    }

    public static class DatabaseInputParameters{

        private int storageInGb = 20;

        private String instanceClass = "db.t3.micro";

        private String mysqlVersion = "8.0.44";

        /**
         * The storage allocated for the database in GB.
         * <p>
         * Default: 20.
         */
        public DatabaseInputParameters withStorageInGb(int storageInGb){
            this.storageInGb = storageInGb;
            return this;
        }

        /**
         * The class of the database instance.
         * <p>
         * Default: "db.t2.micro".
         */
        public DatabaseInputParameters withInstanceClass(String instanceClass) {
            Objects.requireNonNull(instanceClass);
            this.instanceClass = instanceClass;
            return this;
        }

        /**
         * The version of the MySQL database.
         * <p>
         * Default: "8.0.44".
         */
        public DatabaseInputParameters withMySQLVersion(String mysqlVersion) {
            Objects.requireNonNull(mysqlVersion);
            this.mysqlVersion = mysqlVersion;
            return this;
        }

    }

}
