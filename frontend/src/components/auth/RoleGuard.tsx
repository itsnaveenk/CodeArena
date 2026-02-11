import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types';
import { toast } from '@/lib/toast';
import { useEffect, useRef } from 'react';

interface RoleGuardProps {
  allowedRoles: Role[];
}

export function RoleGuard({ allowedRoles }: RoleGuardProps) {
  const { user } = useAuthStore();
  const hasShownToast = useRef(false);

  const hasAccess = user && allowedRoles.includes(user.role);

  useEffect(() => {
    if (!hasAccess && !hasShownToast.current) {
      hasShownToast.current = true;
      toast.error('Access denied. You do not have permission to view this page.');
    }
  }, [hasAccess]);

  if (!hasAccess) {
    return <Navigate to="/problems" replace />;
  }

  return <Outlet />;
}
