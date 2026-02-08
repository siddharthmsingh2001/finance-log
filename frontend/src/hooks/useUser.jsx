import { useContext } from "react";
import { AppContext } from "../context/AppContext.jsx";

export const useUser = () => {
    const context = useContext(AppContext);

    if (!context) {
        throw new Error("useUser must be used within an AppContextProvider");
    }

    return {
        user: context.user,
        isLoading: context.isLoading,
        setUser: context.setUser,
        clearUser: context.clearUser
    };
};