package com.financelog.service;

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