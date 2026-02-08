package com.financelog.backend.controller;

import com.financelog.backend.dto.APIResponse;
import com.financelog.backend.dto.UserDto;
import com.financelog.backend.entity.AuthenticatedUser;
import com.financelog.backend.service.OrchestrationService;
import com.financelog.backend.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/${api.user.version}/user")
public class UserController {

    private final OrchestrationService orchestrationService;
    private final S3Service s3Service;

    @Value("${api.aws.s3.bucket}")
    String bucketName;

    public UserController(
            OrchestrationService orchestrationService,
            S3Service s3Service
    ){
        this.orchestrationService = orchestrationService;
        this.s3Service = s3Service;
    }

    @GetMapping
    public ResponseEntity<APIResponse<UserDto>> me(@AuthenticationPrincipal AuthenticatedUser user){
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.of(
                            HttpStatus.UNAUTHORIZED,
                            null,
                            "User is not authenticated"
                    ));
        }
        UserDto dto = orchestrationService.getOrCreateUser(user);
        return ResponseEntity.ok(
                APIResponse.ok(dto, "User session is valid")
        );
    }

    @GetMapping("/upload-url")
    public ResponseEntity<APIResponse<Map<String, String>>> getUploadUrl(
            @RequestParam String extension,
            @RequestParam String contentType
    ) {
        // Generate a unique filename
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String uploadUrl = s3Service.generatePresignedUploadUrl(fileName, contentType);

        // The actual public URL where the image will live after upload
        String publicUrl = String.format("https://%s.s3.amazonaws.com/profiles/%s", bucketName, fileName);

        return ResponseEntity.ok(APIResponse.ok(Map.of(
                "uploadUrl", uploadUrl,
                "publicUrl", publicUrl
        ), "Presigned URL generated"));
    }
}