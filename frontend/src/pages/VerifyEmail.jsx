import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import axiosPublic from "../util/axiosPublic";
import { API_ENDPOINTS } from "../util/apiEndpoints";
import toast from "react-hot-toast";
import Header from "../components/Header";
import { assets } from "../assets/assets";

const VerifyEmail = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [code, setCode] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const email = location.state?.email || "";

    const handleVerify = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await axiosPublic.post(API_ENDPOINTS.CONFIRM, { email, code });
            toast.success("Email verified! You can now login.");
            navigate("/login");
        } catch (err) {
            toast.error("Invalid code. Please try again.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleResend = async () => {
        try {
            await axiosPublic.post(API_ENDPOINTS.RESEND_CODE, { email });
            toast.success("New code sent to your email.");
        } catch (err) {
            toast.error("Failed to resend code. Try again later.");
        }
    };

    return (
        <div className="h-screen w-full flex flex-col">
            <Header />

            <div className="flex-grow w-full relative flex items-center justify-center overflow-hidden">

                {/* Background */}
                <img
                    src={assets.bg}
                    alt="Background"
                    className="absolute inset-0 w-full h-full object-cover scale-105 blur-md"
                />

                <div className="relative z-10 w-full max-w-md px-6">
                    <div className="bg-white bg-opacity-95 backdrop-blur-sm rounded-lg shadow-2xl p-8">

                        <h2 className="text-2xl font-bold text-center mb-2">
                            Verify Your Email
                        </h2>

                        <p className="text-sm text-slate-700 text-center mb-6">
                            Enter the 6-digit code sent to <br />
                            <span className="font-semibold text-black">
                                {email}
                            </span>
                        </p>

                        <form onSubmit={handleVerify} className="space-y-4">
                            <input
                                type="text"
                                maxLength="6"
                                value={code}
                                onChange={(e) => setCode(e.target.value)}
                                className="w-full border border-gray-300 focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none p-3 text-center text-2xl tracking-widest rounded-lg transition-all"
                                placeholder="000000"
                            />

                            <button
                                disabled={isLoading}
                                className={`w-full py-3 text-lg font-bold rounded-lg transition-all 
                                    ${isLoading
                                    ? "bg-green-400 cursor-not-allowed"
                                    : "bg-green-600 hover:bg-green-700 text-white shadow-md"
                                }`}
                            >
                                {isLoading ? "Verifying..." : "VERIFY"}
                            </button>
                        </form>

                        <button
                            type="button"
                            onClick={handleResend}
                            className="mt-6 w-full text-purple-600 text-sm font-medium hover:underline transition"
                        >
                            Didn’t receive the code? Resend
                        </button>

                    </div>
                </div>
            </div>
        </div>
    );
};

export default VerifyEmail;
