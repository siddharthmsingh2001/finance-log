package com.financelog.platform.database;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.network.NetworkOutputParameters;
import com.financelog.network.NetworkParameterStore;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.util.Collections;

/**
 * CDK Construct that provisions a Bastion Host (Jump Host) inside a VPC.
 *
 * <p>
 * The Bastion Host is an EC2 instance deployed into a public subnet and is used
 * as a secure entry point into the VPC for accessing private resources such as
 * the database. It enables SSH tunneling from a developer's local machine to
 * private services (e.g. MySQL running on RDS).
 * </p>
 *
 * <p>
 * Key characteristics:
 * <ul>
 *   <li>The Bastion Host lives in a <b>public subnet</b>.</li>
 *   <li>SSH access (port 22) is restricted to a specific CIDR range
 *       (typically the developer's public IP).</li>
 *   <li>The database remains private and only allows inbound traffic from
 *       the Bastion Host's security group.</li>
 *   <li>An existing EC2 Key Pair is attached to the instance for SSH access.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This construct relies on infrastructure metadata stored in AWS SSM Parameter Store,
 * following the Stratospheric.dev pattern:
 * <ul>
 *   <li>VPC ID, public subnets, and availability zones from the Network stack</li>
 *   <li>Database security group ID from the Database stack</li>
 * </ul>
 * </p>
 */
public class BastionConstruct extends Construct {

    /**
     * Creates a Bastion Host inside the given application environment.
     *
     * @param scope the parent construct (typically a Stack)
     * @param constructId logical ID of this construct within the stack
     * @param applicationEnvironment encapsulates application name and deployment stage
     *                               (used for naming and tagging resources)
     * @param bastionInputParameters user-specific inputs such as SSH key name
     *                               and allowed SSH CIDR
     */
    public BastionConstruct(
            Construct scope,
            String constructId,
            ApplicationEnvironment applicationEnvironment,
            BastionInputParameters bastionInputParameters
    ){
        super(scope, constructId);

        // --- Loading required Output Parameters ---
        NetworkOutputParameters networkOutputParameters = NetworkParameterStore.load(this,applicationEnvironment.getDeploymentStage());
        DatabaseOutputParameters databaseOutputParameters = DatabaseParameterStore.load(this, applicationEnvironment);

        // --- BastionHost Security Group ---
        CfnSecurityGroup bastionHostSg = CfnSecurityGroup.Builder.create(this, "BastionHostSecurityGroup")
                .groupName(applicationEnvironment.prefix("bastion-host-sg"))
                .groupDescription("SecurityGroup containing the BastionHost")
                .vpcId(networkOutputParameters.getVpcId())
                .build();

        // --- Ingress rule to allow access to BastionHost ---
        CfnSecurityGroupIngress bastionHostSGIngress =  CfnSecurityGroupIngress.Builder.create(this,"BastionHostSecurityGroupIngress")
                .groupId(bastionHostSg.getAttrGroupId())
                .fromPort(22)
                .toPort(22)
                .ipProtocol("TCP")
                .cidrIp(bastionInputParameters.allowedSshCidr)
                .build();

        // --- Ingress rule to allow access to Database ---
        CfnSecurityGroupIngress databaseSGIngress = CfnSecurityGroupIngress.Builder.create(this, "DatabaseSecurityGroupIngress")
                .sourceSecurityGroupId(bastionHostSg.getAttrGroupId())
                .groupId(databaseOutputParameters.getSecurityGroupId())
                .fromPort(3306)
                .toPort(3306)
                .ipProtocol("TCP")
                .build();

        CfnInstance bastionHost = CfnInstance.Builder.create(this, "BastionHost")
                .instanceType("t3.nano")
                .imageId(
                        StringParameter.valueForStringParameter(
                                this,
                                "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
                        )
                )
                .subnetId(Fn.select(0, networkOutputParameters.getPublicSubnets()))
                .keyName(bastionInputParameters.keyName)
                .securityGroupIds(
                        Collections.singletonList(bastionHostSg.getAttrGroupId())
                )
                .build();

        CfnOutput.Builder.create(this, "BastionHostPublicIp")
                        .value(bastionHost.getAttrPublicIp())
                        .build();

        applicationEnvironment.tag(this);
    }

    /**
     * Input parameters required to configure the Bastion Host.
     *
     * <p>
     * These values are intentionally supplied at deploy time (via CDK context or CLI)
     * and are not stored in source control.
     * </p>
     */
    public static class BastionInputParameters{

        /**
         * Name of an existing EC2 Key Pair.
         *
         * <p>
         * The key pair must already exist in the target AWS account and region.
         * CDK does not create or manage the private key; it is used only to
         * associate the public key with the EC2 instance.
         * </p>
         */
        private final String keyName;

        /**
         * CIDR block that is allowed to SSH into the Bastion Host.
         *
         * <p>
         * Typically this is the developer's public IP in the form {@code x.x.x.x/32}.
         * Restricting this value is critical for security.
         * </p>
         */
        private final String allowedSshCidr;

        public BastionInputParameters(String keyName, String allowedSshCidr) {
            this.keyName = keyName;
            this.allowedSshCidr = allowedSshCidr;
        }
    }

}
