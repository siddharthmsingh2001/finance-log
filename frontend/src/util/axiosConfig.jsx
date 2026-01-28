import axios from "axios";
import {BASE_URL, API_ENDPOINTS} from "./apiEndpoints.js"

const axiosConfig = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
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
            window.location.href = BASE_URL+API_ENDPOINTS.AUTH;
        }
        return Promise.reject(error);
    }
);

export default axiosConfig;