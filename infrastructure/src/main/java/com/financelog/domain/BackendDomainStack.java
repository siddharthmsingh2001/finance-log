package com.financelog.domain;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.network.NetworkOutputParameters;
import com.financelog.network.NetworkParameterStore;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancerAttributes;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;
import software.constructs.Construct;

/**
 * CDK stack responsible for managing DNS records for the api domain.
 *
 * <p>
 * This stack:
 * <ul>
 *   <li>Imports an existing Route 53 public hosted zone</li>
 *   <li>Imports an existing Application Load Balancer (ALB)</li>
 *   <li>Creates an alias A record pointing the api domain to the ALB</li>
 * </ul>
 *
 * <p>
 * The stack assumes that:
 * <ul>
 *   <li>The hosted zone already exists in Route 53</li>
 *   <li>The Application Load Balancer was created by a previously deployed stack</li>
 *   <li>Load balancer attributes are stored in SSM Parameter Store</li>
 * </ul>
 */
public class BackendDomainStack extends Stack {

    /**
     * Creates a new {@link BackendDomainStack}.
     *
     * @param scope                  parent construct (usually the CDK {@link App})
     * @param constructId            logical identifier for the stack
     * @param awsEnvironment         AWS account and region configuration
     * @param applicationEnvironment application-level environment metadata
     * @param hostedZoneDomain       root domain of the Route 53 hosted zone
     * @param applicationDomain      fully qualified domain name to be routed to the ALB
     */
    public BackendDomainStack(
            final Construct scope,
            final String constructId,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final String hostedZoneDomain,
            final String applicationDomain
    ){
        /*
         * Initializes the stack with a deterministic name and target environment.
         */
        super(
                scope,
                constructId,
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("backend-domain-stack"))
                        .env(awsEnvironment)
                        .build()
        );

        /*
         * Imports an existing public Route 53 hosted zone.
         *
         * <p>
         * The hosted zone is expected to have been created during
         * domain registration or via a separate infrastructure stack.
         */
        IHostedZone hostedZone = HostedZone.fromLookup(
                this,
                "HostedZone",
                HostedZoneProviderProps.builder()
                        .domainName(hostedZoneDomain)
                        .build()
        );

        /*
         * Loads network-related output parameters from the SSM Parameter Store.
         *
         * <p>
         * These parameters are expected to have been stored by a previously
         * deployed network stack and include:
         * <ul>
         *   <li>Application Load Balancer ARN</li>
         *   <li>Load balancer security group ID</li>
         *   <li>Canonical hosted zone ID</li>
         *   <li>DNS name of the load balancer</li>
         * </ul>
         */
        final NetworkOutputParameters outputParameters = NetworkParameterStore.load(
                this, applicationEnvironment.getDeploymentStage()
        );

        /*
         * Imports an existing Application Load Balancer using known attributes.
         *
         * <p>
         * This does not create a new load balancer. It creates a reference
         * to an already existing ALB so it can be used as a DNS target.
         */
        IApplicationLoadBalancer applicationLoadBalancer = ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
                this,
                "LoadBalancer",
                ApplicationLoadBalancerAttributes.builder()
                        .loadBalancerArn(outputParameters.getLoadBalancerArn())
                        .securityGroupId(outputParameters.getLoadbalancerSecurityGroupId())
                        .loadBalancerCanonicalHostedZoneId(outputParameters.getLoadBalancerCanonicalHostedZoneId())
                        .loadBalancerDnsName(outputParameters.getLoadBalancerDnsName())
                        .build()
        );


        /*
         * Creates an alias A record in Route 53 that maps the application
         * domain name to the Application Load Balancer.
         *
         * <p>
         * Alias records are AWS-managed pointers and do not incur
         * additional DNS query charges.
         */
        ARecord aRecord = ARecord.Builder.create(this, "AlbARecord")
                .recordName(applicationDomain)
                .zone(hostedZone)
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(applicationLoadBalancer)))
                .build();

        /*
         * Applies application-wide tags to all resources in this stack.
         */
        applicationEnvironment.tag(this);
    }

}