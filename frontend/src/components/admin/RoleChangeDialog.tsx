import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { Role } from '@/types';
import type { UserListItem } from '@/lib/api';

interface RoleChangeDialogProps {
  user: UserListItem | null;
  newRole: Role | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isLoading?: boolean;
}

const getRoleBadgeColor = (role: Role) => {
  switch (role) {
    case 'ADMIN':
      return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
    case 'PROBLEM_SETTER':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
    default:
      return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200';
  }
};

const getRoleLabel = (role: Role): string => {
  switch (role) {
    case 'ADMIN':
      return 'Admin';
    case 'PROBLEM_SETTER':
      return 'Problem Setter';
    default:
      return 'User';
  }
};

export function RoleChangeDialog({
  user,
  newRole,
  open,
  onOpenChange,
  onConfirm,
  isLoading = false,
}: RoleChangeDialogProps) {
  if (!user || !newRole) {
    return null;
  }

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Change User Role"
      description={
        <div className="space-y-4">
          <p>
            Are you sure you want to change the role for{' '}
            <span className="font-semibold">{user.name}</span>?
          </p>
          <div className="flex items-center gap-4 justify-center py-2">
            <div className="text-center">
              <p className="text-xs text-muted-foreground mb-1">Current Role</p>
              <Badge
                className={cn(
                  'px-3 py-1',
                  getRoleBadgeColor(user.role)
                )}
              >
                {getRoleLabel(user.role)}
              </Badge>
            </div>
            <div className="text-muted-foreground">→</div>
            <div className="text-center">
              <p className="text-xs text-muted-foreground mb-1">New Role</p>
              <Badge
                className={cn(
                  'px-3 py-1',
                  getRoleBadgeColor(newRole)
                )}
              >
                {getRoleLabel(newRole)}
              </Badge>
            </div>
          </div>
          <p className="text-sm text-muted-foreground">
            This action will immediately change the user's permissions on the platform.
          </p>
        </div>
      }
      confirmLabel="Change Role"
      onConfirm={onConfirm}
      isLoading={isLoading}
    />
  );
}

export default RoleChangeDialog;
