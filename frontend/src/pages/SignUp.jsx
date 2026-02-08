import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axiosPublic from "../util/axiosPublic"; // Use the public instance
import { API_ENDPOINTS } from "../util/apiEndpoints.js";
import toast from "react-hot-toast";
import Header from "../components/Header.jsx";
import Input from "../components/Input.jsx";
import ProfilePhotoSelector from "../components/ProfilePhotoSelector.jsx";
import {assets} from "../assets/assets.js";

const Signup = () => {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [profilePhoto, setProfilePhoto] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);

        // 1. Prepare Name DTO
        const nameParts = fullName.trim().split(" ");
        const givenName = nameParts[0];
        const familyName = nameParts.length > 1 ? nameParts.slice(1).join(" ") : "-";

        let finalProfileImageUrl = "default_image"; // Fallback

        try {
            // 2. IMAGE UPLOAD FLOW (If a photo is selected)
            if (profilePhoto) {
                const extension = profilePhoto.name.split('.').pop();

                // A. Get Presigned URL from UserController
                // Note: Since this is likely protected, you might need axiosPrivate,
                // but for Signup, usually, we allow a public 'pre-signup' upload endpoint
                // or just skip image on signup and do it in 'Profile Edit'.
                // Assuming this is a public utility for now:
                const { data: { data: uploadDetails } } = await axiosPublic.get(API_ENDPOINTS.GET_UPLOAD_URL, {
                    params: { extension, contentType: profilePhoto.type }
                });

                // B. Upload directly to S3
                await fetch(uploadDetails.uploadUrl, {
                    method: "PUT",
                    body: profilePhoto,
                    headers: { "Content-Type": profilePhoto.type }
                });

                finalProfileImageUrl = uploadDetails.publicUrl;
            }

            // 3. ACTUAL SIGNUP
            const signupPayload = {
                email,
                password,
                givenName,
                familyName,
                profileImageUrl: finalProfileImageUrl
            };

            const response = await axiosPublic.post(API_ENDPOINTS.SIGNUP, signupPayload);

            if (response.status === 201) {
                toast.success("Registration successful! Check your email.");
                navigate("/verify", { state: { email } });
            }

        } catch (err) {
            const errorData = err.response?.data;

            // SMART ROUTING: Handle User Already Exists
            if (errorData?.errorCode === "AUTH_USER_ALREADY_EXISTS") {
                toast.error("An account with this email already exists.");
                setTimeout(() => navigate("/login"), 2000);
                return;
            }

            toast.error(errorData?.causeMsg || "Something went wrong. Please try again.");
            console.error("Signup Error:", err);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="h-screen w-full flex flex-col">
            <Header />

            <div className="flex-grow w-full relative flex items-center justify-center overflow-hidden">

                {/* Background image with blur */}
                <img
                    src={assets.bg} // or create signup_bg if you want different
                    alt="Background"
                    className="absolute inset-0 w-full h-full object-cover filter blur-sm"
                />

                <div className="relative z-10 w-full max-w-md px-6">
                    <div className="bg-white bg-opacity-95 backdrop-blur-sm rounded-lg shadow-2xl p-8">

                        <h2 className="text-2xl font-bold text-center mb-6">
                            Create Account
                        </h2>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <ProfilePhotoSelector
                                image={profilePhoto}
                                setImage={setProfilePhoto}
                            />

                            <Input
                                label="Full Name"
                                value={fullName}
                                onChange={(e) => setFullName(e.target.value)}
                                placeholder="John Doe"
                            />

                            <Input
                                label="Email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="john@example.com"
                            />

                            <Input
                                label="Password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="********"
                            />

                            <button
                                disabled={isLoading}
                                className={`w-full py-3 text-lg font-bold rounded-lg transition-all 
                                ${isLoading
                                    ? "bg-purple-400 cursor-not-allowed"
                                    : "bg-purple-600 hover:bg-purple-700 text-white shadow-md"
                                }`}
                            >
                                {isLoading ? "Creating Account..." : "SIGN UP"}
                            </button>
                        </form>

                        <p className="mt-6 text-center text-sm text-slate-800">
                            Already have an account?{" "}
                            <Link
                                to="/login"
                                className="font-bold text-purple-600 underline hover:text-purple-800 transition-colors"
                            >
                                Login
                            </Link>
                        </p>

                    </div>
                </div>
            </div>
        </div>
    );
};

export default Signup;