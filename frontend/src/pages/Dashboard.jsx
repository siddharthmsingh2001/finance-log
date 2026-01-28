import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Wallet, WalletCards, Coins } from "lucide-react";
import {addThousandsSeparator} from "../util/util.js";
import axiosConfig from "../util/axiosConfig.jsx";
import {API_ENDPOINTS} from "../util/apiEndpoints.js";
import { useUser } from "../hooks/useUser";
import DashboardLayout from "../components/DashboardLayout.jsx";
import InfoCard from "../components/InfoCard.jsx";
import RecentTransactions from "../components/RecentTransaction.jsx";
import FinanceOverview from "../components/FinanceOverview.jsx";


function Transactions(props) {
    return null;
}

const Dashboard = () =>{

    const { user, loading: userLoading } = useUser();
    const navigate = useNavigate();

    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if(!user || userLoading) return;

        const fetchDashboard = async ()=>{
            setLoading(true);
            try{
                const res = await axiosConfig.get(API_ENDPOINTS.DASHBOARD);
                setDashboard(res);
            } catch (err){
                console.error(err);
                toast.error("Failed to load dashboard");
            } finally{
                setLoading(false);
            }
        };

        fetchDashboard();
        },[user, userLoading]);

    if (userLoading || loading) {
        return <div className="p-10 text-center">Loading dashboard…</div>;
    }

    return (
        <DashboardLayout activeMenu="Dashboard">
            <div className="my-5 mx-auto">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <InfoCard
                        icon={<WalletCards />}
                        label="Total Balance"
                        value={addThousandsSeparator(dashboard?.totalBalance || 0)}
                        color="bg-purple-800"
                    />
                    <InfoCard
                        icon={<Wallet />}
                        label="Total Income"
                        value={addThousandsSeparator(dashboard?.totalIncome || 0)}
                        color="bg-green-800"
                    />
                    <InfoCard
                        icon={<Coins />}
                        label="Total Expense"
                        value={addThousandsSeparator(dashboard?.totalExpense || 0)}
                        color="bg-red-800"
                    />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                    <RecentTransactions
                        transactions={dashboard?.recentTransactions}
                        onMore={() => navigate("/expense")}
                    />

                    <FinanceOverview
                        totalBalance={dashboard?.totalBalance || 0}
                        totalIncome={dashboard?.totalIncome || 0}
                        totalExpense={dashboard?.totalExpense || 0}
                    />

                    <Transactions
                        transactions={dashboard?.recent5Expenses || []}
                        onMore={() => navigate("/expense")}
                        type="expense"
                        title="Recent Expenses"
                    />

                    <Transactions
                        transactions={dashboard?.recent5Incomes || []}
                        onMore={() => navigate("/income")}
                        type="income"
                        title="Recent Incomes"
                    />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default Dashboard;