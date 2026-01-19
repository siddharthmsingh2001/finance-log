package com.financelog.domain;

import com.financelog.service.FrontendOutputParameters;
import com.financelog.service.FrontendParameterStore;
import com.financelog.service.FrontendStack;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.constructs.Construct;
import com.financelog.core.ApplicationEnvironment;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.route53.*;

/**
 * CDK stack responsible for managing DNS records for the application domain.
 *
 * <p>
 * This stack:
 * <ul>
 *   <li>Imports an existing Route 53 public hosted zone</li>
 *   <li>Imports an existing Cloudfront Distribution (CDN)</li>
 *   <li>Creates an alias A record pointing the application domain to the CDN</li>
 * </ul>
 *
 * <p>
 * The stack assumes that:
 * <ul>
 *   <li>The hosted zone already exists in Route 53</li>
 *   <li>The Cloudfront Distribution was created by a previously deployed {@link FrontendStack} </li>
 *   <li>Cloudfront Distribution attributes are stored in SSM Parameter Store</li>
 * </ul>
 */
public class FrontendDomainStack extends Stack {

    /**
     * Creates a new {@link FrontendDomainStack}.
     *
     * @param scope                  parent construct (usually the CDK {@link App})
     * @param constructId            logical identifier for the stack
     * @param awsEnvironment         AWS account and region configuration
     * @param applicationEnvironment application-level environment metadata
     * @param hostedZoneDomain       root domain of the Route 53 hosted zone
     * @param frontendDomainName      fully qualified domain name to be routed to the CDN
     */
    public FrontendDomainStack(
            final Construct scope,
            final String constructId,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final String hostedZoneDomain,
            final String frontendDomainName
    ) {
        super(
                scope,
                constructId,
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("frontend-domain-stack"))
                        .env(awsEnvironment)
                        .build()
        );

        /*
         * 1. Import the existing hosted zone
         */
        IHostedZone hostedZone = HostedZone.fromLookup(
                this,
                "HostedZone",
                HostedZoneProviderProps.builder()
                        .domainName(hostedZoneDomain)
                        .build()
        );


        /*
         * Loading from the ParameterStore the outputs necessary for finding the Distribution.
         */
        FrontendOutputParameters frontendOutputs =
                FrontendParameterStore.load(this, applicationEnvironment.getDeploymentStage());


        /*
         * 2. Import the CloudFront distribution from attributes.
         * This converts the domain name (String) into an IDistribution object.
         */
        IDistribution distribution = Distribution.fromDistributionAttributes(
                this,
                "Distribution",
                DistributionAttributes.builder()
                        .domainName(frontendOutputs.getCloudFrontDomainName())
                        .distributionId(frontendOutputs.getCloudFrontDistributionId())
                        .build()
        );

        /*
         * 3. Create the alias A record
         */
        ARecord.Builder.create(this, "CdnARecord")
                .zone(hostedZone)
                .recordName(frontendDomainName)
                .target(RecordTarget.fromAlias(new CloudFrontTarget(distribution)))
                .build();

        /*
         * Applies application-wide tags to all resources in this stack.
         */
        applicationEnvironment.tag(this);
    }
}