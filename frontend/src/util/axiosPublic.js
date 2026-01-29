import axios from "axios";
import {BASE_URL, API_ENDPOINTS} from "./apiEndpoints.js"

const axiosPublic = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
    }
});

export default axiosPublic;