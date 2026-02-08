import { createContext, useState, useEffect } from "react";
import axiosPublic from "../util/axiosPublic";
import { API_ENDPOINTS } from "../util/apiEndpoints.js";

export const AppContext = createContext();

export const AppContextProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    // This runs ONCE when the entire React app starts
    useEffect(() => {
        const verifySession = async () => {
            try {
                const response = await axiosPublic.get(API_ENDPOINTS.GET_ME);
                // Remember: Backend returns APIResponse<UserDto>
                // So we need response.data.data
                if (response.data && response.data.data) {
                    setUser(response.data.data);
                }
            } catch (error) {
                console.log("No active session found.");
                setUser(null);
            } finally {
                setIsLoading(false);
            }
        };

        verifySession();
    }, []);

    const clearUser = () => setUser(null);

    const contextValue = {
        user,
        setUser,
        isLoading,
        clearUser
    };

    return (
        <AppContext.Provider value={contextValue}>
            {children}
        </AppContext.Provider>
    );
};