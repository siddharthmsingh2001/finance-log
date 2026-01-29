import {useContext, useEffect, useState} from "react";
import {AppContext} from "../context/AppContext.jsx";
import axiosPublic from "../util/axiosPublic";
import {API_ENDPOINTS} from "../util/apiEndpoints.js";

export const useUser = () => {
    const {user, setUser, clearUser} = useContext(AppContext);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadUser = async () => {
            try {
                const response = await axiosPublic.get(API_ENDPOINTS.USER_INFO);
                setUser(response.data);
            } catch {
                clearUser();
            } finally {
                setIsLoading(false);
            }
        };
        loadUser();
    }, []);

    return { user, isLoading };
}