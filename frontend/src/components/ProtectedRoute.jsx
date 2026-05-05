import { Navigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";
import { getAuthenticatedHomePath } from "./routeGuardUtils";

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { authReady, isAuthenticated, user } = useAuth();

  if (!authReady) {
    return <div className="min-h-[55vh]" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={getAuthenticatedHomePath(user)} replace />;
  }

  return children;
};

export default ProtectedRoute;
