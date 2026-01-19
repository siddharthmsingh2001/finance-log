package com.financelog.service;

/**
 * A central registry of keys used for SSM Parameter Store.
 * <p>
 * Why this exists: Instead of hardcoding "cloudFrontDistributionId" in multiple
 * places, we define it here once. If we ever want to change the key name,
 * we only have to change it in this file.
 * </p>
 */
public final class FrontendOutputs {

    private FrontendOutputs() {
        // prevent instantiation
    }
    /** The key used to store/retrieve the CloudFront Distribution ID. */
    public static final String PARAMETER_CLOUDFRONT_DISTRIBUTION_ID = "cloudFrontDistributionId";

    /** The key used to store/retrieve the auto-generated CloudFront Domain Name. */
    public static final String PARAMETER_CLOUDFRONT_DOMAIN_NAME = "cloudFrontDomainName";
}