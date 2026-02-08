package com.financelog.backend.service;

public interface S3Service {

    String generatePresignedUploadUrl(String fileName, String contentType);

}
