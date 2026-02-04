package com.financelog.platform.storage;

public class S3OutputParameters {

    private final String s3BucketName;

    public S3OutputParameters(String s3BucketName){
        this.s3BucketName = s3BucketName;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

}
