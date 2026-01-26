import {useContext, useEffect, useState} from "react";
import {AppContext} from "../context/AppContext.jsx";
import axiosConfig from "../util/axiosConfig.jsx";

export const useUser = () => {
    const {user, setUser, clearUser} = useContext(AppContext);
    const [isLoading, setIsLoading] = useState(true); // Start as true!

    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                const response = await axiosConfig.get("/v1/user-info");
                setUser(response.data);
            } catch (error) {
                clearUser();
            } finally {
                setIsLoading(false); // Stop loading no matter what
            }
        };
        fetchUserInfo();
    }, []);

    return { user, isLoading }; // Return both!
}