import toast from "react-hot-toast";

export const handleApiError = (error, navigate) => {
    const errorData = error.response?.data;

    if (!errorData) {
        toast.error("Network error. Please check your connection.");
        return;
    }

    switch (errorData.errorCode) {
        case "AUTH_USER_ALREADY_EXISTS":
            toast.error("Email already registered. Redirecting to login...");
            if (navigate) setTimeout(() => navigate("/login"), 1500);
            break;
        case "AUTH_INVALID_CREDENTIALS":
            toast.error("Invalid email or password.");
            break;
        case "AUTH_USER_NOT_CONFIRMED":
            toast.error("Please verify your email first.");
            if (navigate) navigate("/verify", { state: { email: errorData.email } });
            break;
        default:
            toast.error(errorData.causeMsg || "An unexpected error occurred.");
    }
};