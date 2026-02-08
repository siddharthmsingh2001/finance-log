export const BASE_URL = "https://api.finance-log.com";

const AUTH_VERSION = 'v1';
const USER_VERSION = 'v1';

export const API_ENDPOINTS = {
    // Auth Controller - /api/v1/auth
    LOGIN: `/api/${AUTH_VERSION}/auth/login`,
    SIGNUP: `/api/${AUTH_VERSION}/auth/signup`,
    CONFIRM: `/api/${AUTH_VERSION}/auth/confirm`,
    RESEND_CODE: `/api/${AUTH_VERSION}/auth/resend-code`, // Map this in your AuthController

    // User Controller - /api/v1/user
    GET_ME: `/api/${USER_VERSION}/user`,

    /**
     * S3 Presigned URL Endpoint
     * Used during signup to get a 'hall pass' for direct S3 upload.
     */
    GET_UPLOAD_URL: `/api/${USER_VERSION}/user/upload-url`,
};