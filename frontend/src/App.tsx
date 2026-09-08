import { BrowserRouter, Routes, Route } from "react-router-dom";
import LoginRoute from "./app/login/Page";
import Dashboard from "./components/dashboard/Dashboard";
import RequireAuth from "./components/auth/Requireauth";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginRoute />} />
                <Route
                    path="/*"
                    element={
                        <RequireAuth>
                            <Dashboard />
                        </RequireAuth>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;