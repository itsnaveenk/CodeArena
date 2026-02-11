import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient, type UsersFilter } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { toast } from '@/lib/toast';
import type { Role } from '@/types';

export function useUsers(filter?: UsersFilter) {
  return useQuery({
    queryKey: queryKeys.admin.users.list(filter),
    queryFn: () => getApiClient().getUsers(filter),
  });
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: Role }) =>
      getApiClient().updateUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      toast.success('User role updated successfully');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update user role');
    },
  });
}
