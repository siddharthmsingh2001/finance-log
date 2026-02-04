package com.financelog.platform.storage;

import com.financelog.core.ApplicationEnvironment;
import com.financelog.core.DeploymentStage;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import java.util.List;

/**
 * S3Construct handles the creation of the S3 bucket used for user uploads (profile photos).
 * It configures CORS to allow direct uploads from the frontend and sets up public access
 * so images can be served via direct URLs.
 */
public class S3Construct extends Construct {

    private final IBucket userUploadsBucket;

    public S3Construct(
            Construct scope,
            String constructId,
            ApplicationEnvironment applicationEnvironment
    ){
        super(scope, constructId);

        /*
         * 1. STORAGE BUCKET CONFIGURATION
         * We enable CORS so the React app (running on a different domain)
         * can perform HTTP PUT requests to upload files directly.
         */
        this.userUploadsBucket = Bucket.Builder.create(this, "UserUploadsBucket")
                .bucketName(applicationEnvironment.prefix("user-uploads"))
                .blockPublicAccess(BlockPublicAccess.Builder.create()
                        .blockPublicAcls(false)
                        .blockPublicPolicy(false)
                        .ignorePublicAcls(false)
                        .restrictPublicBuckets(false)
                        .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .encryption(BucketEncryption.S3_MANAGED)
                .cors(List.of(CorsRule.builder()
                        .allowedOrigins(List.of("http://localhost:5173","https://app.finance-log.com"))
                        .allowedMethods(List.of(HttpMethods.PUT, HttpMethods.POST, HttpMethods.GET))
                        .allowedHeaders(List.of("*"))
                        .exposedHeaders(List.of("ETag"))
                        .build()))
                .build();

        StringParameter.Builder.create(this, "S3BucketName")
                .parameterName(applicationEnvironment.prefix("s3-"+S3Outputs.BUCKET_NAME))
                .stringValue(this.userUploadsBucket.getBucketName())
                .build();
    }

    /**
     * @return The bucket interface for use in other parts of the infrastructure (like IAM policies).
     */
    public IBucket getBucket() {
        return this.userUploadsBucket;
    }

}