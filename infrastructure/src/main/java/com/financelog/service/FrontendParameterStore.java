package com.financelog.service;

import com.financelog.core.DeploymentStage;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * Utility for fetching existing Frontend infrastructure details from AWS SSM.
 */
public final class FrontendParameterStore {

    private FrontendParameterStore() {
    }

    /**
     * Reaches out to AWS SSM and pulls all frontend-related parameters into a
     * single {@link FrontendOutputParameters} object.
     * @param scope The CDK construct scope (usually 'this').
     * @param stage The current environment (Dev/Prod) to ensure we pull the correct values.
     * @return A populated parameters object.
     */
    public static FrontendOutputParameters load(
            Construct scope,
            DeploymentStage stage
    ) {
        return new FrontendOutputParameters(
                get(scope, stage, FrontendOutputs.PARAMETER_CLOUDFRONT_DISTRIBUTION_ID),
                get(scope, stage, FrontendOutputs.PARAMETER_CLOUDFRONT_DOMAIN_NAME)
        );
    }

    /**
     * Performs the actual lookup of a single string from SSM.
     */
    private static String get(Construct scope, DeploymentStage stage, String key) {
        return StringParameter.fromStringParameterName(
                scope,
                key,
                parameterName(stage, key)
        ).getStringValue();
    }

    /**
     * Standardizes the naming convention for parameters: {stage}-frontend-{key}
     * Example: "dev-frontend-cloudFrontDistributionId"
     */
    private static String parameterName(DeploymentStage stage, String key) {
        return stage.getName() + "-frontend-" + key;
    }
}