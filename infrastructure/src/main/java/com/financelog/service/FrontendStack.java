package com.financelog.service;

import com.financelog.core.DeploymentStage;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin; // Note the new class
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import com.financelog.core.ApplicationEnvironment;
import java.util.List;

public class FrontendStack extends Stack {

    private final Distribution distribution;
    private final Bucket frontendBucket;

    public FrontendStack(
            final Construct scope,
            final String constructId,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final String certificateArn
    ) {
        super(scope, constructId, StackProps.builder()
                .stackName(applicationEnvironment.prefix("frontend-stack"))
                .env(awsEnvironment)
                .build());

        // 1. S3 Bucket for React assets
        this.frontendBucket = Bucket.Builder.create(this, "FrontendBucket")
                .bucketName(applicationEnvironment.prefix("frontend-assets"))
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .encryption(BucketEncryption.S3_MANAGED)
                // OAC requires Object Ownership to be "Bucket Owner Enforced"
                .objectOwnership(ObjectOwnership.BUCKET_OWNER_ENFORCED)
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .build();

        // 2. CloudFront Distribution using the new OAC L2 Construct
        this.distribution = Distribution.Builder.create(this, "FrontendDistribution")
                .domainNames(List.of("app.finance-log.com"))
                .defaultRootObject("index.html")
                .certificate(Certificate.fromCertificateArn(this, "FrontendCertificate", certificateArn))
                .minimumProtocolVersion(SecurityPolicyProtocol.TLS_V1_2_2021)
                .defaultBehavior(BehaviorOptions.builder()
                        // This single line replaces all the manual OAC and Policy code!
                        .origin(S3BucketOrigin.withOriginAccessControl(frontendBucket))
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .compress(true)
                        .cachePolicy(CachePolicy.CACHING_OPTIMIZED)
                        .build())
                .errorResponses(List.of(
                        ErrorResponse.builder().httpStatus(403).responseHttpStatus(200).responsePagePath("/index.html").ttl(Duration.seconds(0)).build(),
                        ErrorResponse.builder().httpStatus(404).responseHttpStatus(200).responsePagePath("/index.html").ttl(Duration.seconds(0)).build()
                ))
                .build();

        putParameter(
                "CloudFrontDistributionIdParameter",
                applicationEnvironment.getDeploymentStage(),
                FrontendOutputs.PARAMETER_CLOUDFRONT_DISTRIBUTION_ID,
                distribution.getDistributionId()
        );

        putParameter(
                "CloudFrontDomainNameParameter",
                applicationEnvironment.getDeploymentStage(),
                FrontendOutputs.PARAMETER_CLOUDFRONT_DOMAIN_NAME,
                distribution.getDistributionDomainName()
        );

        // Outputs (Keep these as they are useful for CI/CD)
        new CfnOutput(this, "CloudFrontDomainName", CfnOutputProps.builder()
                .exportName(applicationEnvironment.prefix("frontend-cloudfront-domain"))
                .value(distribution.getDistributionDomainName())
                .build());

        new CfnOutput(this, "CloudFrontDistributionId", CfnOutputProps.builder()
                .exportName(applicationEnvironment.prefix("frontend-cloudfront-id"))
                .value(distribution.getDistributionId())
                .build());

        applicationEnvironment.tag(this);
    }

    private static String createParameterName(DeploymentStage stage, String key) {
        return stage.getName() + "-frontend-" + key;
    }

    private void putParameter(String constructId, DeploymentStage stage, String name, String value) {
        StringParameter.Builder.create(this, constructId)
                .parameterName(createParameterName(stage, name))
                .stringValue(value)
                .build();
    }
}