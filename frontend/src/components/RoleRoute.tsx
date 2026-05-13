import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { hasAnyRole, type AppRole } from '../auth/roles';

type RoleRouteProps = {
  allowed: AppRole[];
};

export function RoleRoute({ allowed }: RoleRouteProps) {
  const { user, loading } = useAuth();

  if (loading) {
    return null;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!hasAnyRole(user, allowed)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
