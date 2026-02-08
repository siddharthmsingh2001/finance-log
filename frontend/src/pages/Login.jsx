import { useContext, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { assets } from "../assets/assets.js";
import Input from "../components/Input.jsx";
import { validateEmail } from "../util/validation.js";
import axiosPublic from "../util/axiosPublic"; // Updated to use Public instance
import { API_ENDPOINTS } from "../util/apiEndpoints.js";
import { AppContext } from "../context/AppContext.jsx";
import { LoaderCircle } from "lucide-react";
import Header from "../components/Header.jsx";
import toast from "react-hot-toast";

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const { setUser } = useContext(AppContext);

    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError("");

        // 1. Basic Validation
        if (!validateEmail(email)) {
            setError("Please enter a valid email address");
            setIsLoading(false);
            return;
        }

        if (!password.trim()) {
            setError("Please enter your password");
            setIsLoading(false);
            return;
        }

        try {
            // 2. LOGIN API call
            // We use axiosPublic because the user is not yet authenticated.
            const response = await axiosPublic.post(API_ENDPOINTS.LOGIN, {
                email,
                password,
            });

            /* * NOTE: Since we are using Spring Session Cookies:
             * - There is NO 'token' to store in localStorage.
             * - The browser automatically saves the JSESSIONID cookie.
             * - response.data matches your backend APIResponse<UserDto>
             */
            const apiResponse = response.data; // This is the APIResponse object
            const userDto = apiResponse.data;  // This is the actual UserDto

            if (userDto) {
                setUser(userDto);
                toast.success(apiResponse.message || "Login successful!");
                navigate("/dashboard");
            }
        } catch (err) {
            // 3. Structured Error Handling
            const errorData = err.response?.data;

            // If the backend returned our custom ErrorResponseDto
            if (errorData && errorData.causeMsg) {
                setError(errorData.causeMsg);
            } else if (err.response?.status === 401) {
                setError("Invalid email or password.");
            } else {
                setError("An unexpected error occurred. Please try again.");
                console.error('Login Error:', err);
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="h-screen w-full flex flex-col">
            <Header />
            <div className="flex-grow w-full relative flex items-center justify-center overflow-hidden">
                {/* Background image with blur */}
                <img src={assets.bg} alt="Background" className="absolute inset-0 w-full h-full object-cover filter blur-sm" />

                <div className="relative z-10 w-full max-w-md px-6">
                    <div className="bg-white bg-opacity-95 backdrop-blur-sm rounded-lg shadow-2xl p-8">
                        <h3 className="text-2xl font-semibold text-black text-center mb-2">
                            Welcome Back
                        </h3>
                        <p className="text-sm text-slate-700 text-center mb-8">
                            Please enter your details to log in
                        </p>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <Input
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                label="Email Address"
                                placeholder="name@example.com"
                                type="text"
                            />

                            <Input
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                label="Password"
                                placeholder="*********"
                                type="password"
                            />

                            {error && (
                                <div className="text-red-800 text-sm text-center bg-red-50 p-2 rounded border border-red-200 animate-pulse">
                                    {error}
                                </div>
                            )}

                            <button
                                disabled={isLoading}
                                className={`w-full py-3 text-lg font-bold rounded-lg transition-all flex items-center justify-center gap-2 
                                    ${isLoading
                                    ? 'bg-purple-400 cursor-not-allowed'
                                    : 'bg-purple-600 hover:bg-purple-700 text-white shadow-md'}`}
                                type="submit"
                            >
                                {isLoading ? (
                                    <>
                                        <LoaderCircle className="animate-spin w-5 h-5" />
                                        Authenticating...
                                    </>
                                ) : ("LOGIN")}
                            </button>

                            <p className="text-sm text-slate-800 text-center mt-6">
                                Don't have an account?{" "}
                                <Link to="/signup" className="font-bold text-purple-600 underline hover:text-purple-800 transition-colors">
                                    Signup
                                </Link>
                            </p>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;