import axios from "axios";
import {BASE_URL, API_ENDPOINTS} from "./apiEndpoints.js"

const axiosPrivate = axios.create({
    baseURL: "http://localhost:8080/api/v1",
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
    }
});

axiosPrivate.interceptors.response.use(
    res => res,
    err => {
        if (err.response?.status === 401) {
            window.location.href = BASE_URL+API_ENDPOINTS.AUTH;
        }
        return Promise.reject(err);
    }
);

export default axiosPrivate;