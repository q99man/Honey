import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import AdminActivitiesPage from "./pages/AdminActivitiesPage";
import AdminAuditLogsPage from "./pages/AdminAuditLogsPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminPlacesPage from "./pages/AdminPlacesPage";
import AdminPoliciesPage from "./pages/AdminPoliciesPage";
import AdminReportsPage from "./pages/AdminReportsPage";
import AdminUsersPage from "./pages/AdminUsersPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Redirect root page to admin dashboard */}
        <Route path="/" element={<Navigate to="/admin" replace />} />
        
        {/* Admin Routes */}
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/activities" element={<AdminActivitiesPage />} />
        <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
        <Route path="/admin/policies" element={<AdminPoliciesPage />} />
        <Route path="/admin/places" element={<AdminPlacesPage />} />
        <Route path="/admin/reports" element={<AdminReportsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />

        {/* Fallback route - redirect to admin */}
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
