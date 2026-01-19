package com.financelog.service;

/**
 * A Data Transfer Object (DTO) that holds the results of an SSM lookup.
 * <p>
 * Instead of passing individual strings around, we bundle the Distribution ID
 * and Domain Name together. This makes method signatures much cleaner.
 * </p>
 */
public class FrontendOutputParameters {

    private final String cloudFrontDistributionId;
    private final String cloudFrontDomainName;

    public FrontendOutputParameters(
            String cloudFrontDistributionId,
            String cloudFrontDomainName
    ) {
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        this.cloudFrontDomainName = cloudFrontDomainName;
    }

    public String getCloudFrontDistributionId() {
        return cloudFrontDistributionId;
    }

    public String getCloudFrontDomainName() {
        return cloudFrontDomainName;
    }
}