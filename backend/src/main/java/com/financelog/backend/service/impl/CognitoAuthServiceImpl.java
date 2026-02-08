package com.financelog.backend.service.impl;

import com.financelog.backend.entity.AuthenticatedUser;
import com.financelog.backend.exception.ResponseStatus;
import com.financelog.backend.exception.custom.AuthException;
import com.financelog.backend.service.CognitoAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CognitoAuthServiceImpl implements CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String clientId;
    private final String clientSecret;

    public CognitoAuthServiceImpl(
            @Value("${api.aws.cognito.clientId}") String clientId,
            @Value("${api.aws.cognito.clientSecret}") String clientSecret,
            @Value("${api.aws.region}") String region
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public AuthenticatedUser login(String email, String password) {

        try{
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", email,
                            "PASSWORD", password,
                            "SECRET_HASH", calculateSecretHash(email)
                    ))
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            // Get User Attributes using the Access Token
            GetUserResponse userResponse = cognitoClient.getUser(GetUserRequest.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .build());

            Map<String, String> attributes = userResponse.userAttributes().stream()
                    .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

            return new AuthenticatedUser(
                    UUID.fromString(attributes.get("sub")),
                    attributes.get("email"),
                    attributes.get("given_name"),
                    attributes.get("family_name"),
                    attributes.getOrDefault("picture", "default_image"),
                    authResponse.authenticationResult().accessToken(),
                    authResponse.authenticationResult().idToken()
            );
        } catch(NotAuthorizedException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_INVALID_CREDENTIALS, cause);
        } catch (UserNotConfirmedException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_USER_NOT_CONFIRMED, cause);
        } catch(UserNotFoundException cause){
            throw  new AuthException(cause.getMessage(), ResponseStatus.AUTH_USER_NOT_FOUND, cause);
        }
    }

    @Override
    public void signUp(String email, String password, String givenName, String familyName, String profileImageUrl){
        try{
            SignUpRequest request = SignUpRequest.builder()
                    .clientId(clientId)
                    .secretHash(calculateSecretHash(email))
                    .username(email)
                    .password(password)
                    .userAttributes(
                            AttributeType.builder()
                                    .name("email")
                                    .value(email)
                                    .build(),
                            AttributeType.builder()
                                    .name("given_name")
                                    .value(givenName)
                                    .build(),
                            AttributeType.builder()
                                    .name("family_name")
                                    .value(familyName)
                                    .build(),
                            AttributeType.builder()
                                    .name("picture")
                                    .value(profileImageUrl)
                                    .build()
                    )
                    .build();
            cognitoClient.signUp(request);
        } catch (UsernameExistsException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_USER_ALREADY_EXISTS, cause);
        } catch (InvalidPasswordException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_PASSWORD_POLICY_VIOLATED, cause);
        } catch (CognitoIdentityProviderException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_UNKNOWN, cause);
        }
    }

    @Override
    public void confirmSignUp(String email, String code){
        try{
            ConfirmSignUpRequest request = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .secretHash(calculateSecretHash(email))
                    .username(email)
                    .confirmationCode(code)
                    .build();
            cognitoClient.confirmSignUp(request);
        } catch (CodeMismatchException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_CODE_MISMATCH, cause);
        } catch (ExpiredCodeException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_CODE_EXPIRED, cause);
        } catch (UserNotFoundException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_USER_NOT_FOUND, cause);
        } catch (CognitoIdentityProviderException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_UNKNOWN, cause);
        }
    }

    @Override
    public void resendConfirmationCode(String email){
        try{
            ResendConfirmationCodeRequest request = ResendConfirmationCodeRequest.builder()
                    .clientId(clientId)
                    .secretHash(calculateSecretHash(email))
                    .username(email)
                    .build();

            cognitoClient.resendConfirmationCode(request);
        } catch (UserNotFoundException cause){
            throw new AuthException(cause.getMessage(), ResponseStatus.AUTH_USER_NOT_FOUND, cause);
        } catch (CognitoIdentityProviderException cause){
            throw new AuthException(cause.getMessage(),ResponseStatus.AUTH_UNKNOWN, cause);
        }
    }

    private String calculateSecretHash(String userName) {
        SecretKeySpec signingKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }


}
