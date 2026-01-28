import { useContext } from "react";
import { AppContext } from "../context/AppContext";
import Menubar from "./Menubar";
import Sidebar from "./Sidebar";

const DashboardLayout = ({ children, activeMenu }) => {
    const { user } = useContext(AppContext);

    if (!user) return null;

    return (
        <>
            <Menubar activeMenu={activeMenu} />
            <div className="flex">
                <div className="max-[1080px]:hidden">
                    <Sidebar activeMenu={activeMenu} />
                </div>
                <div className="grow mx-5">{children}</div>
            </div>
        </>
    );
};

export default DashboardLayout;