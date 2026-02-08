import {BrowserRouter, Navigate, Routes, Route} from "react-router-dom";
import Income from "./pages/Income.jsx";
import Expense from "./pages/Expense.jsx";
import Category from "./pages/Category.jsx";
import Filter from "./pages/Filter.jsx";
import LandingPage from "./pages/LandingPage.jsx";
import {Toaster} from "react-hot-toast";
import {useUser} from "./hooks/useUser.jsx";
import SignUp from "./pages/SignUp.jsx";
import VerifyEmail from "./pages/VerifyEmail.jsx";
import Login from "./pages/Login.jsx";
import Dashboard from "./pages/Dashboard.jsx";

const App = () => {
    return(
        <>
            <Toaster />
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<AuthGuard />} />
                    <Route path="/home" element={<LandingPage />} />
                    <Route path="/signup" element={
                        <GuestGuard>
                            <SignUp />
                        </GuestGuard>
                    } />
                    <Route path="/verify" element={<VerifyEmail />} />
                    <Route path="/login" element={
                        <GuestGuard>
                            <Login />
                        </GuestGuard>
                    } />
                    <Route path="/dashboard" element={
                        <PrivateRoute>
                            <Dashboard />
                        </PrivateRoute>
                    } />
                    <Route path="/income" element={<Income />} />
                    <Route path="/expense" element={<Expense />} />
                    <Route path="/category" element={<Category />} />
                    <Route path="/filter" element={<Filter />} />
                </Routes>
            </BrowserRouter>
        </>
    )
}

const GuestGuard = ({ children }) => {
    const { user, isLoading } = useUser();

    // 1. Still checking session? Show nothing or a spinner
    if (isLoading) {
        return <div className="h-screen flex items-center justify-center">Loading...</div>;
    }

    // 2. If user is already logged in, send them to the dashboard
    if (user) {
        return <Navigate to="/dashboard" replace />;
    }

    // 3. If no session, allow them to see the login/signup page
    return children;
};

const PrivateRoute = ({ children }) => {
    const { user, isLoading } = useUser();

    if (isLoading) {
        return <div className="h-screen flex items-center justify-center">Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    return children;
};

const AuthGuard = () => {
    const { user, isLoading } = useUser(); // We add 'isLoading' to your hook

    // 1. While we are waiting for Spring Boot to check the Cookie
    if (isLoading) {
        return <div className="h-screen flex items-center justify-center">Loading...</div>;
    }

    // 2. Once we have an answer:
    return user ? <Navigate to="/dashboard" /> : <Navigate to="/home" />;
}

export default App;