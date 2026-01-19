package com.financelog.service;

import com.financelog.core.DeploymentStage;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public final class FrontendParameterStore {

    private FrontendParameterStore() {
    }

    public static FrontendOutputParameters load(
            Construct scope,
            DeploymentStage stage
    ) {
        return new FrontendOutputParameters(
                get(scope, stage, FrontendOutputs.PARAMETER_CLOUDFRONT_DISTRIBUTION_ID),
                get(scope, stage, FrontendOutputs.PARAMETER_CLOUDFRONT_DOMAIN_NAME)
        );
    }

    private static String get(Construct scope, DeploymentStage stage, String key) {
        return StringParameter.fromStringParameterName(
                scope,
                key,
                parameterName(stage, key)
        ).getStringValue();
    }

    private static String parameterName(DeploymentStage stage, String key) {
        return stage.getName() + "-frontend-" + key;
    }
}