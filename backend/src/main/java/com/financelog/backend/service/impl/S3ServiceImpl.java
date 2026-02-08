package com.financelog.backend.service.impl;

import com.financelog.backend.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3ServiceImpl implements S3Service {

    private final S3Presigner presigner;
    private final String bucketName;

    public S3ServiceImpl(
            @Value("${api.aws.s3.bucket}") String bucketName,
            @Value("${api.aws.region}") String region) {
        this.bucketName = bucketName;
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String generatePresignedUploadUrl(String fileName, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("profiles/" + fileName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

}


