import {BrowserRouter, Navigate, Routes, Route} from "react-router-dom";
import Home from "./pages/Home.jsx";
import Income from "./pages/Income.jsx";
import Expense from "./pages/Expense.jsx";
import Category from "./pages/Category.jsx";
import Filter from "./pages/Filter.jsx";
import LandingPage from "./pages/LandingPage.jsx";
import {Toaster} from "react-hot-toast";
import {useUser} from "./hooks/useUser.jsx";

const App = () => {
    return(
        <>
            <Toaster />
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<AuthGuard />} />
                    <Route path="/home" element={<LandingPage />} />
                    <Route path="/dashboard" element={<Home />} />
                    <Route path="/income" element={<Income />} />
                    <Route path="/expense" element={<Expense />} />
                    <Route path="/category" element={<Category />} />
                    <Route path="/filter" element={<Filter />} />
                </Routes>
            </BrowserRouter>
        </>
    )
}

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