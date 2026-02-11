import { useState } from 'react';
import { useUsers, useUpdateUserRole } from '@/hooks/useUsers';
import { DataTable, type ColumnDef } from '@/components/ui/data-table';
import { SearchInput } from '@/components/ui/search-input';
import { SingleFilterDropdown } from '@/components/ui/filter-dropdown';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { MoreHorizontal, UserCog, Shield, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types';
import type { UserListItem } from '@/lib/api';

const PAGE_SIZE = 10;

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'USER', label: 'User' },
  { value: 'PROBLEM_SETTER', label: 'Problem Setter' },
  { value: 'ADMIN', label: 'Admin' },
];

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

const getRoleLabel = (role: Role) => {
  switch (role) {
    case 'ADMIN':
      return 'Admin';
    case 'PROBLEM_SETTER':
      return 'Problem Setter';
    default:
      return 'User';
  }
};

export function UserManagementTable() {
  const { user: currentUser } = useAuthStore();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<Role | null>(null);
  const [roleChangeDialog, setRoleChangeDialog] = useState<{
    open: boolean;
    user: UserListItem | null;
    newRole: Role | null;
  }>({ open: false, user: null, newRole: null });

  const { data, isLoading } = useUsers({
    page,
    size: PAGE_SIZE,
    search: search || undefined,
    role: roleFilter || undefined,
  });

  const updateRoleMutation = useUpdateUserRole();

  const handleRoleChange = (user: UserListItem, newRole: Role) => {
    if (user.id === currentUser?.id) return; // Prevent self-role-change
    setRoleChangeDialog({ open: true, user, newRole });
  };

  const confirmRoleChange = () => {
    if (roleChangeDialog.user && roleChangeDialog.newRole) {
      updateRoleMutation.mutate(
        { userId: roleChangeDialog.user.id, role: roleChangeDialog.newRole },
        {
          onSuccess: () => setRoleChangeDialog({ open: false, user: null, newRole: null }),
        }
      );
    }
  };

  const columns: ColumnDef<UserListItem>[] = [
    {
      id: 'name',
      header: 'Name',
      cell: (row) => (
        <div>
          <div className="font-medium">{row.name}</div>
          <div className="text-sm text-muted-foreground">{row.email}</div>
        </div>
      ),
    },
    {
      id: 'role',
      header: 'Role',
      cell: (row) => (
        <span
          className={cn(
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getRoleBadgeColor(row.role)
          )}
        >
          {getRoleLabel(row.role)}
        </span>
      ),
    },
    {
      id: 'problemsSolved',
      header: 'Problems Solved',
      cell: (row) => row.problemsSolved,
    },
    {
      id: 'createdAt',
      header: 'Joined',
      cell: (row) => new Date(row.createdAt).toLocaleDateString(),
    },
    {
      id: 'actions',
      header: '',
      cell: (row) => {
        const isSelf = row.id === currentUser?.id;

        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" disabled={isSelf}>
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem disabled className="text-xs text-muted-foreground">
                Change Role
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => handleRoleChange(row, 'USER')}
                disabled={row.role === 'USER'}
              >
                <User className="h-4 w-4 mr-2" />
                User
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => handleRoleChange(row, 'PROBLEM_SETTER')}
                disabled={row.role === 'PROBLEM_SETTER'}
              >
                <UserCog className="h-4 w-4 mr-2" />
                Problem Setter
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => handleRoleChange(row, 'ADMIN')}
                disabled={row.role === 'ADMIN'}
              >
                <Shield className="h-4 w-4 mr-2" />
                Admin
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        );
      },
      className: 'w-12',
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row gap-4">
        <SearchInput
          placeholder="Search by name or email..."
          value={search}
          onChange={setSearch}
          className="w-full sm:w-64"
        />
        <SingleFilterDropdown
          label="Role"
          options={ROLE_OPTIONS}
          value={roleFilter}
          onChange={setRoleFilter}
        />
      </div>

      <DataTable
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        pagination={
          data
            ? {
                page: data.page,
                pageSize: data.size,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
              }
            : undefined
        }
        onPageChange={setPage}
        emptyMessage="No users found"
      />

      <ConfirmDialog
        open={roleChangeDialog.open}
        onOpenChange={(open) =>
          !open && setRoleChangeDialog({ open: false, user: null, newRole: null })
        }
        title="Change User Role"
        description={`Are you sure you want to change ${roleChangeDialog.user?.name}'s role from ${getRoleLabel(roleChangeDialog.user?.role || 'USER')} to ${getRoleLabel(roleChangeDialog.newRole || 'USER')}?`}
        confirmLabel="Change Role"
        onConfirm={confirmRoleChange}
        isLoading={updateRoleMutation.isPending}
      />
    </div>
  );
}

export default UserManagementTable;
