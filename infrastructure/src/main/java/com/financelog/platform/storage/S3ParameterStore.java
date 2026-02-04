package com.financelog.platform.storage;

import com.financelog.core.ApplicationEnvironment;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class S3ParameterStore {

    private S3ParameterStore(){}

    public static S3OutputParameters load(
            Construct scope,
            ApplicationEnvironment applicationEnvironment
    ){
        return new S3OutputParameters(
                get(scope, applicationEnvironment, S3Outputs.BUCKET_NAME)
        );
    }

    private static String get(Construct scope, ApplicationEnvironment applicationEnvironment, String key) {
        return StringParameter.fromStringParameterName(
                scope,
                key,
                parameterName(applicationEnvironment, key)
        ).getStringValue();
    }

    private static String parameterName(ApplicationEnvironment applicationEnvironment, String key) {
        return applicationEnvironment.prefix("s3-"+key);
    }
}
