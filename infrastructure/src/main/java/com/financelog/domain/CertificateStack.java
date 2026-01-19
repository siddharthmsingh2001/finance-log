package com.financelog.domain;

import com.financelog.core.ApplicationEnvironment;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

/**
 * CDK stack responsible for provisioning an SSL/TLS certificate
 * using AWS Certificate Manager (ACM).
 *
 * <p>
 * The certificate is validated using DNS validation via Route 53
 * and is intended to be reused by other stacks (e.g. ALB, CloudFront,
 * API Gateway) through CloudFormation exports.
 * </p>
 */
public class CertificateStack extends Stack {

    /**
     * Creates a new {@link CertificateStack}.
     *
     * @param scope                  parent construct (usually the CDK {@link App})
     * @param constructId            logical identifier for the stack
     * @param awsEnvironment         AWS account and region configuration
     * @param applicationEnvironment application-level environment metadata
     * @param applicationDomain      fully qualified domain name for the certificate
     * @param hostedZoneDomain       root domain of the Route 53 hosted zone
     */
    public CertificateStack(
            final Construct scope,
            final String constructId,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final String applicationDomain,
            final String hostedZoneDomain
    ){

        /*
         * Initializes the stack with a deterministic name and target environment.
         */
        super (
                scope,
                constructId,
                StackProps.builder()
                        .stackName(applicationEnvironment
                                .prefix("certificate-stack"))
                        .env(awsEnvironment)
                        .build()
        );

        /*
         * Retrieve the Hosted Zone(A container that holds all the DNS
         * records for a specific domain) that was created when we registered
         * our domain through Route 53
         */
        IHostedZone hostedZone = HostedZone.fromLookup(
                this,
                "HostedZone",
                HostedZoneProviderProps.builder().domainName(hostedZoneDomain).build()
        );

        /*
         * Creating a new SSL certificate validated via DNS(a DNS-validated certificate
         * makes use of the fact that only the owner of a domain has control over its DNS
         * entries)
         * By doing this we are able to create a DNS TXT record for our domain
         */
        ICertificate webCertificate = Certificate.Builder.create(this, "WebCertificate")
                .domainName(applicationDomain)
                .validation(CertificateValidation.fromDns(hostedZone))
                .build();

        /*
         * Exports the certificate ARN so it can be imported
         * and reused by other CloudFormation stacks like NetworkStack
         * or FrontendDomainStack
         */
        new CfnOutput(
                this,
                "SslCertificateArn",
                CfnOutputProps.builder()
                        .exportName("sslCertificateArn")
                        .value(webCertificate.getCertificateArn())
                        .description("Certificate ARN which is to be used for setting up HTTPs")
                        .build()
        );
    }
}
