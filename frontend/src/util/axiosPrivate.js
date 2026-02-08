import axios from "axios";
import {BASE_URL, API_ENDPOINTS} from "./apiEndpoints.js"

const axiosPrivate = axios.create({
    baseURL: BASE_URL,
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
            window.location.href = "/login";
        }
        return Promise.reject(err);
    }
);

export default axiosPrivate;