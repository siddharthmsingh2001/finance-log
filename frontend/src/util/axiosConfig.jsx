import axios from "axios";

const axiosConfig = axios.create({
    baseURL: "https://api.finance-log.com/api/", // Your Spring Boot URL
    withCredentials: true,           // MANDATORY: Sends the Session Cookie to the Backend
    headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
    }
});

// We NO LONGER need the request interceptor that adds "Bearer" tokens!
// The browser handles the cookie automatically.

axiosConfig.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            // If the session expired in Redis/Spring, send them to login
            window.location.href = "https://api.finance-log.com/api/oauth2/authorization/cognito";
        }
        return Promise.reject(error);
    }
);

export default axiosConfig;